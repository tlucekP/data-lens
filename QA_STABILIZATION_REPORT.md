# MVP Stabilization QA Report

Date: 2026-03-06
Scope: Step 2 (stabilization QA pass) + Step 3 (automated test baseline) after MVP scope freeze.

## Environment
- Workspace: project root
- Execution mode: terminal/sandbox + manual desktop run.

## Checklist Results (README Manual Test Workflow)

1. Start app with `mvn javafx:run`.
- Status: PASS (technical smoke)
- Evidence: command stayed running for 15s without startup failure (`README_RUN_STATUS=RUNNING_AFTER_15S`).

2. Verify UI structure (toolbar, overview, analysis, warnings, summary).
- Status: PASS
- Evidence: manual UI check completed; all required sections visible.

3. Load `samples/sample_valid.csv` and verify updates.
- Status: PASS
- Evidence: overview numbers, warnings, and summary updated correctly.

4. Click `Reload` and verify state consistency.
- Status: PASS
- Evidence: reload did not break UI state. Note: no explicit visual signal confirming reprocessing.

5. Load `samples/sample_valid.xlsx` and verify first relevant sheet analysis.
- Status: PASS
- Evidence: first relevant sheet analyzed and shown correctly.

6. Load `samples/sample_broken.xlsx` and verify safe error handling.
- Status: PASS
- Evidence: safe error dialog shown (invalid Office Open XML archive), app remained stable.

7. Re-open valid file after broken-file scenario.
- Status: PASS
- Evidence: app recovered and processed valid file normally.

8. Run packaging helper and verify `dist/DataLens` exists.
- Status: PASS
- Evidence:
  - Command: `powershell -ExecutionPolicy Bypass -File .\\scripts\\package-windows.ps1`
  - Result: `Packaging finished in ...\\dist`
  - Output confirmed: `dist\\DataLens` exists.

9. Optional MSI build when WiX is available.
- Status: PASS
- Evidence:
  - Command: `powershell -ExecutionPolicy Bypass -File .\\scripts\\package-windows.ps1 -Type msi`
  - Result: `Packaging finished in ...\\dist`
  - Output confirmed: `dist\\DataLens-0.1.0.msi` exists.

## Additional Automated Checks
- Sample files present: PASS (`sample_valid.csv`, `sample_valid.xlsx`, `sample_broken.xlsx`).
- Maven clean package: PASS.
- JUnit suite: PASS (`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`).

## Fixes Applied During QA
1. Packaging script JDK selection robustness fix.
- File: `scripts/package-windows.ps1`
- Change: normalize `$jdkCandidates` to an array before indexing; use count check for presence.
- Impact: default packaging commands now work even when only one JDK candidate is available.

## Step 3 Test Baseline (Automated)
- Added JUnit 5 test baseline in `src/test/java` for:
  - `FileValidators`
  - `CsvLoader`
  - `XlsxLoader`
  - `DataProfiler`
- Command executed: `mvn test`
- Result: PASS (`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`)

## Non-Blocking UX Improvements (Backlog)
1. Increase Warnings section height (about +50%) and reduce Column Analysis vertical space.
2. Add explicit Reload feedback (for example timestamp or toast) to confirm reprocessing happened.
3. Show total workbook sheet count in Dataset Overview when XLSX is loaded.

## MVP QA Conclusion
- MVP validation is complete for current scope.
- No blocking defects found in tested flows.
- Remaining items are UX/product enhancements and can be scheduled after MVP release.

