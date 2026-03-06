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
- WiX-based `exe` and `msi` packaging is prepared, but still environment-dependent. In this machine state, `light.exe` exits with code `216`, so installer packaging is not yet enabled in CI.

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