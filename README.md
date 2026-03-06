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

The project is prepared so the next step can package the app through `jpackage` into `.exe` or `.msi`.

Suggested flow:

1. Build the fat jar with `mvn clean package`.
2. Use `jpackage` with `--input target`, `--main-jar datalens-0.1.0-SNAPSHOT-fat.jar`, and `--main-class com.datalens.app.MainApp`.
3. Add Windows-specific icons and installer metadata in the next iteration.

Example shape of the command:

```bash
jpackage ^
  --type exe ^
  --name DataLens ^
  --input target ^
  --main-jar datalens-0.1.0-SNAPSHOT-fat.jar ^
  --main-class com.datalens.app.MainApp
```

## GitHub Releases publishing plan

1. Create or update the GitHub repository.
2. Run `mvn clean package`.
3. Upload the fat jar to a GitHub Release.
4. In the packaging iteration, upload the `jpackage` generated `.exe` or `.msi` as release assets.

## Manual test workflow

1. Start the app with `mvn javafx:run`.
2. Verify the UI shows the top toolbar, dataset overview, column analysis, warnings, and summary.
3. Load `samples/sample_valid.csv` and confirm overview numbers, warnings, and summary update.
4. Click `Reload` and confirm the same file is reprocessed without breaking UI state.
5. Load `samples/sample_valid.xlsx` and confirm the first relevant sheet is analyzed.
6. Load `samples/sample_broken.xlsx` and confirm the app shows a safe error message instead of crashing.
7. Re-open a valid file after the broken-file check and confirm the app still behaves normally.

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
