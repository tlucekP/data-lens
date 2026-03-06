# DataLens

DataLens is a local JavaFX desktop MVP for quick profiling of CSV and XLSX datasets. It runs fully offline and focuses on simple, readable diagnostics instead of charts, editing, or cloud services.

## Stack

- Java 21
- JavaFX
- Maven
- Apache Commons CSV
- Apache POI

## Features in this MVP

- Load `.csv` and `.xlsx` files from disk
- Reload the last loaded file
- Dataset overview with rows, columns, empty rows, and header detection
- Column analysis with type detection, missing values, unique counts, and short stats
- Warning engine for common data quality issues
- Local heuristic summary generator with an analyst-style output
- Sample input files for manual testing

## What Follows in the Next Version

Planned product direction for the next version:

- Export features for summary and analysis outputs
- Charts for quick visual inspection of numeric and categorical fields
- Basic data editing for selected cell values and simple corrections
- Drag and drop file loading directly into the main window

These items are not part of the current MVP yet.

## Run in development

1. Install Java 21 and Maven.
2. From the project root run:

```bash
mvn javafx:run
```

## Maven build

```bash
mvn clean package
```

## Fat JAR

The Maven build creates a shaded artifact:

```text
target/datalens-0.1.0-SNAPSHOT-fat.jar
```

Run it with:

```bash
java -jar target/datalens-0.1.0-SNAPSHOT-fat.jar
```

Note: the current dependency setup targets Windows JavaFX artifacts because the MVP is prepared for Windows distribution first.

## jpackage / Windows packaging plan

The repository includes a Windows packaging helper:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

Defaults:

- builds the fat jar if needed
- creates a `dist` directory
- generates a Windows `app-image`

Optional types:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Type exe
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Type msi
```

For `exe` and `msi`, install WiX Toolset locally into `.tools/wix` with:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-wix.ps1
```

Current status:

- `app-image` packaging is verified in this repository.
- WiX-based `exe` and `msi` packaging is verified locally.
- The packaging script uses a workspace-local `.jpackage-temp/` directory to avoid `%TEMP%`/Windows Defender issues seen on some machines.
- A local `light.exe` wrapper suppresses MSI validation (`-sval`) and reports errors to the console instead of crashing with a modal dialog.

## GitHub automation

The repository includes GitHub Actions workflows for:

- CI build on push and pull request
- release build on version tags like `v0.1.0`

CI workflow output:

- regular jar
- fat jar

Release workflow output:

- fat jar
- Windows app image

## GitHub Releases publishing plan

1. Push to GitHub.
2. Create a version tag such as `v0.1.0`.
3. Push the tag.
4. Let the release workflow build and attach artifacts automatically.

## Manual test workflow

1. Start the app with `mvn javafx:run`.
2. Verify the UI shows the top toolbar, dataset overview, column analysis, warnings, and summary.
3. Load `samples/sample_valid.csv` and confirm overview numbers, warnings, and summary update.
4. Click `Reload` and confirm the same file is reprocessed without breaking UI state.
5. Load `samples/sample_valid.xlsx` and confirm the first relevant sheet is analyzed.
6. Load `samples/sample_broken.xlsx` and confirm the app shows a safe error message instead of crashing.
7. Re-open a valid file after the broken-file check and confirm the app still behaves normally.
8. Run `powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1` and confirm `dist/DataLens` is created.
9. If WiX is installed locally, run `powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Type msi` only after validating WiX on the target machine.

## Included sample files

- `samples/sample_valid.csv`
- `samples/sample_valid.xlsx`
- `samples/sample_broken.xlsx`

## Known MVP limits

- Only `.csv` and `.xlsx` are supported.
- Only the first relevant XLSX sheet is analyzed.
- No drag and drop.
- No export.
- No charts.
- No data editing.
- Header detection, type detection, and summary text are heuristic.

## Actual Issues (Roadmap)

Project status summary:
- MVP state is functionally OK and releaseable.
- QA baseline is complete (automated tests + manual flow verification).
- Current issues are mainly in maintainability, robustness, and operational reliability.
- No blocking defect was found for current MVP usage.

### Findings by severity

P2 - UI thread blocking during dataset load and profiling
- Problem: file IO + profiling + warning generation + summary generation are executed in JavaFX UI thread, so larger files can freeze the app.
- Location: `src/main/java/com/datalens/ui/MainController.java` (`loadDataset`).
- Impact: weak UX responsiveness, risk of "app not responding" perception.

P2 - Packaging script tied to hardcoded artifact version/name
- Problem: packaging references `datalens-0.1.0-SNAPSHOT-fat.jar` directly; if project version changes, packaging flow can fail.
- Location: `scripts/package-windows.ps1` (fat jar path and `--main-jar`).
- Impact: brittle release process, avoidable CI/CD failures.

P3 - CSV delimiter detector can mis-handle escaped quotes
- Problem: quote state toggling is naive for edge cases with escaped double quotes.
- Location: `src/main/java/com/datalens/util/DelimiterDetector.java` (`countOutsideQuotes`).
- Impact: wrong delimiter detection on tricky CSV input.

P3 - MainController is too broad (god-class tendency)
- Problem: one class mixes UI concerns, validation, loading, profiling orchestration, and error presentation.
- Location: `src/main/java/com/datalens/ui/MainController.java`.
- Impact: harder testing, harder maintenance, rising complexity over time.

P3 - Missing internal diagnostics/logging for runtime failures
- Problem: errors are shown to user, but no internal structured logs are persisted.
- Location: `src/main/java/com/datalens/ui/MainController.java` (`catch` + `showError`).
- Impact: harder production troubleshooting and incident analysis.

### Refactoring roadmap (to execute in next chat)

R1 - Make dataset processing asynchronous
- Scope: move load + profile pipeline off JavaFX UI thread using `Task`/background executor.
- Deliverables:
  - UI stays responsive during processing.
  - Buttons disabled/enabled consistently during run.
  - User sees progress/loading state.
- Acceptance criteria:
  - No UI freeze on larger sample files.
  - Error handling still behaves identically from user perspective.

R2 - Decouple packaging from hardcoded jar version
- Scope: make `package-windows.ps1` dynamically resolve current fat jar (or derive from Maven project version).
- Deliverables:
  - Script works after version bump without manual edits.
  - Clear error if no unique fat jar is found.
- Acceptance criteria:
  - `app-image` and `msi` packaging pass after changing project version.

R3 - Harden CSV delimiter detection
- Scope: improve quote parsing in delimiter detection for escaped quote scenarios.
- Deliverables:
  - robust `countOutsideQuotes` behavior.
  - new unit tests for delimiter edge cases.
- Acceptance criteria:
  - test set includes escaped quote and mixed delimiter edge inputs.

R4 - Split MainController responsibilities
- Scope: introduce service layer for dataset orchestration (load -> profile -> warnings -> summary).
- Deliverables:
  - thinner `MainController` focused on UI state and bindings.
  - orchestration logic unit-testable without JavaFX runtime.
- Acceptance criteria:
  - controller method size/complexity reduced.
  - existing behavior unchanged.

R5 - Add operational diagnostics
- Scope: add lightweight logging for load failures and key pipeline stages.
- Deliverables:
  - structured log lines (at least info/error) for file load attempts and failures.
  - no sensitive data leakage in logs.
- Acceptance criteria:
  - reproducible errors are diagnosable from logs.

R6 - Security and resilience test extension
- Scope: add non-functional tests to improve input hardening.
- Test recommendations:
  - XLSX zip-bomb/compression-ratio stress input (DoS resistance).
  - Fuzz tests for malformed CSV/XLSX files.
  - Very long row/cell payload stress tests (memory/time bounds).
  - Encoding/BOM variants for CSV parsing robustness.
  - XLSX formula/external-link cell handling checks.
- Acceptance criteria:
  - documented pass/fail outcomes and clear limits for safe processing.

### Non-blocking UX backlog (already observed in manual QA)
- Increase Warnings panel height and reduce Column Analysis vertical share.
- Add explicit Reload feedback (timestamp/toast/status).
- Show total workbook sheet count in Dataset Overview for XLSX.

### Implementation note for next chat
- In the next chat, refactoring should follow this roadmap order: R1 -> R2 -> R3 -> R4 -> R5 -> R6.
- Avoid feature expansion during refactor; keep behavior stable unless explicitly required by an issue above.
