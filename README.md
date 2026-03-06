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

## Actual Issues (Status)

Project status summary:
- MVP state is functionally OK and releaseable.
- QA baseline is complete (automated tests + manual flow verification).
- Roadmap items R1 -> R6 were completed on 2026-03-06.
- No blocking defect is currently tracked in the implemented roadmap areas.

### Findings by severity

Resolved P2 - UI thread blocking during dataset load and profiling
- Status: resolved on 2026-03-06.
- Outcome: dataset processing now runs on a background `Task`/executor and the UI exposes loading status with consistent button state handling.

Resolved P2 - Packaging script tied to hardcoded artifact version/name
- Status: resolved on 2026-03-06.
- Outcome: `scripts/package-windows.ps1` now resolves `artifactId` and `version` from `pom.xml`, derives `AppVersion`, discovers the active `*-fat.jar`, and fails clearly on ambiguous artifacts.
- Verification: `powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1` passed and produced `dist/DataLens`.
- Verification scope note: dynamic jar resolution and current-version `app-image` packaging were executed in this repo; explicit version-bump and WiX `msi` replay were not rerun in the same pass.

Resolved P3 - CSV delimiter detector can mis-handle escaped quotes
- Status: resolved on 2026-03-06.
- Outcome: delimiter detection now handles escaped double quotes correctly and is covered by regression tests for escaped-quote and mixed-delimiter inputs.

Resolved P3 - MainController is too broad (god-class tendency)
- Status: resolved on 2026-03-06.
- Outcome: orchestration moved to `DatasetProcessingService`; `MainController` is now focused on UI state, dialogs, and bindings.

Resolved P3 - Missing internal diagnostics/logging for runtime failures
- Status: resolved on 2026-03-06.
- Outcome: structured info/error diagnostics are persisted to `%USERPROFILE%\.datalens\logs\datalens.log` without logging dataset contents.

### Refactoring roadmap

R1 - Make dataset processing asynchronous
- Status: completed on 2026-03-06.
- Result: load + profile pipeline moved off the JavaFX UI thread using `Task` and a background executor.
- Verification: UI status text and button state handling were added while preserving user-facing error dialogs.

R2 - Decouple packaging from hardcoded jar version
- Status: completed on 2026-03-06.
- Result: packaging resolves metadata from `pom.xml`, auto-derives `AppVersion`, and selects the active fat jar dynamically.
- Verification: `app-image` packaging passed in this repository with the refactored script.

R3 - Harden CSV delimiter detection
- Status: completed on 2026-03-06.
- Result: `countOutsideQuotes` now treats escaped double quotes safely.
- Verification: regression tests cover escaped quotes and mixed delimiter edge cases.

R4 - Split MainController responsibilities
- Status: completed on 2026-03-06.
- Result: service-layer orchestration is unit-testable without JavaFX runtime and controller complexity was reduced.
- Verification: `DatasetProcessingServiceTest` exercises the pipeline outside the UI layer.

R5 - Add operational diagnostics
- Status: completed on 2026-03-06.
- Result: file-load attempts, pipeline stages, success, and failures are logged with lightweight structured metadata.
- Verification: diagnostics log path is `%USERPROFILE%\.datalens\logs\datalens.log`.

R6 - Security and resilience test extension
- Status: completed on 2026-03-06.
- Result: the test suite now covers UTF-8 BOM CSV input, long CSV cell payloads, malformed XLSX input, XLSX formula-cell handling, and zip-bomb-like XLSX payload rejection.
- Verification: `mvn test` on 2026-03-06 reported `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`.
- Verified safe-processing limits in-repo:
  - CSV BOM variants: PASS.
  - CSV escaped-quote delimiter edge cases: PASS.
  - CSV long-cell stress at 20,000 characters: PASS.
  - XLSX invalid archive rejection: PASS.
  - XLSX formula evaluation read path: PASS.
  - XLSX zip-bomb-like compressed worksheet payload: PASS (rejected safely).
### Non-blocking UX backlog (already observed in manual QA)
- Increase Warnings panel height and reduce Column Analysis vertical share.
- Add explicit Reload feedback (timestamp/toast/status).
- Show total workbook sheet count in Dataset Overview for XLSX.

### Implementation note
- The roadmap sequence R1 -> R2 -> R3 -> R4 -> R5 -> R6 was completed on 2026-03-06.
- Further changes should keep behavior stable and treat the items above as the current baseline.




