# MVP Scope Freeze Checklist

Date: 2026-03-06

## Goal
Lock MVP scope to stability and release readiness. No new product features beyond original MVP baseline.

## Current Working Tree Delta
- `README.md` (updated to include export feature and adjusted roadmap/limits)
- `src/main/java/com/datalens/ui/MainController.java` (new export action + state)
- `src/main/resources/ui/main-view.fxml` (new `Export Report` button)
- `src/main/java/com/datalens/export/ReportExporter.java` (new exporter)
- `.m2/` (local Maven cache, untracked)

## Scope Decision Matrix
1. `src/main/java/com/datalens/export/ReportExporter.java`
- Decision: DEFER (out of MVP baseline)
- Reason: new feature work; not required for MVP stabilization/release hardening.

2. `src/main/java/com/datalens/ui/MainController.java` (export-related changes)
- Decision: DEFER (out of MVP baseline)
- Reason: UI and flow changes tied to deferred export feature.

3. `src/main/resources/ui/main-view.fxml` (export button)
- Decision: DEFER (out of MVP baseline)
- Reason: UI extension for deferred feature.

4. `README.md` export-related edits
- Decision: DEFER/REVERT
- Reason: documentation should match frozen MVP scope.

5. `.m2/`
- Decision: EXCLUDE from VCS
- Reason: local build cache only.

## Freeze Actions (Step 1 output)
1. Revert export-related code/doc changes to baseline.
2. Add cache ignores so local build artifacts do not pollute status:
- `.m2/`
- `.m2-home/`
3. Confirm clean working tree (or only intentional stabilization changes).

## Done Criteria for Step 1
- No feature-expansion delta remains in git diff.
- `README.md` reflects frozen MVP scope.
- Cache folders are ignored and not staged.
- Team agrees freeze boundary before stabilization work.
