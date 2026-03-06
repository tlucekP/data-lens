# MVP Manual GUI QA Checklist

Date: 2026-03-06
Build under test: current workspace state
Tester: user
Environment: Windows desktop

## Preconditions
1. Open terminal in the project root.
2. Run app: `mvn javafx:run`
3. Keep this checklist open and mark each item.

## Checklist (README items 2-7)

| ID | Test | Expected result | Status (PASS/FAIL) | Notes / Evidence |
|---|---|---|---|---|
| 2 | Verify UI layout | Top toolbar, dataset overview, column analysis, warnings, and summary are visible | PASS | Layout OK. UX note: Warnings panel should be about 50% larger; reduce Column Analysis area. |
| 3 | Load `samples/sample_valid.csv` | Overview numbers, warnings, and summary update correctly | PASS | Behavior OK. |
| 4 | Click `Reload` | Same file is reprocessed, UI stays stable | PASS | Behavior appears stable; no obvious visual change to confirm reload execution. |
| 5 | Load `samples/sample_valid.xlsx` | First relevant sheet is analyzed and displayed | PASS | Behavior OK. Product note: Dataset Overview should show total sheet count in the workbook. |
| 6 | Load `samples/sample_broken.xlsx` | Safe error message shown, app does not crash | PASS | Error dialog shown with safe message (invalid Office Open XML archive), app did not crash. |
| 7 | Re-open valid file after broken file | App recovers and behaves normally | PASS | Recovery works; valid file loads and analysis is shown again. |

## Exit Criteria
- All rows PASS: GUI QA complete for MVP.
- Any FAIL: create issue with exact repro steps and file used.

## Optional Evidence Pack
- Screenshot after step 3 (valid CSV loaded)
- Screenshot after step 6 (error dialog visible)
- Screenshot after step 7 (valid file loaded again)

