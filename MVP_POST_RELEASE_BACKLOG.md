# MVP Post-Release Backlog

Date: 2026-03-06
Source: Manual GUI QA feedback (non-blocking)

## BL-001 Warnings Panel Visibility
- Priority: Medium
- Problem: Warnings area feels too small for practical reading.
- Proposed change: increase Warnings section height by about 50% and reduce Column Analysis vertical space.
- Acceptance criteria:
  - Warnings list shows more entries without immediate scrolling.
  - Column analysis remains usable on standard laptop resolution.

## BL-002 Reload User Feedback
- Priority: Medium
- Problem: Reload has no explicit confirmation signal.
- Proposed change: add visible reload feedback (for example status text, timestamp update, or toast).
- Acceptance criteria:
  - User can clearly see that reload executed.
  - Feedback appears only on successful reload and does not block UI.

## BL-003 Workbook Sheet Count in Overview
- Priority: Low
- Problem: XLSX processing analyzes first relevant sheet but UI does not show total workbook sheet count.
- Proposed change: show workbook sheet count in Dataset Overview when XLSX file is loaded.
- Acceptance criteria:
  - Dataset Overview includes total sheet count for XLSX.
  - CSV behavior remains unchanged.
