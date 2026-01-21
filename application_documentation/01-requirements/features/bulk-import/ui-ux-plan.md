# Bulk Import UI/UX Plan

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-21 |
| **Author** | Agent |

## 1. Overview
This document outlines the User Interface and User Experience requirements for the "Bulk Import of Krithis" orchestration system. The goal is to provide Admins with a robust "Control Center" to manage, monitor, and review large-scale data imports (1000+ records) from CSV manifests.

## 2. User Personas
- **System Admin**: Responsible for starting imports, monitoring technical health (errors, retries), and intervening when the pipeline gets stuck.
- **Content Editor / Data Reviewer**: Responsible for resolving entity conflicts (e.g., matching a scraped Raga to an existing DB Raga) and approving the final data before it goes live.

## 3. Sitemap & Navigation
The Bulk Import module will be located within the existing Admin Console.
- **Route**: `/admin/bulk-import`
- **Navigation Menu**: "Bulk Import" (under "Data Management" section)

**Hierarchy:**
1.  **Dashboard (Index)**: List of all import batches.
2.  **New Batch Wizard**: Modal/Page to upload CSV and start a process.
3.  **Batch Detail**: Deep dive into a specific batch (Metrics, Logs, Controls).
4.  **Task Review**: Interface for manual intervention (Entity Resolution / Final Approval).

## 4. Key Screens & Detailed UX

### 4.1. Dashboard (Batch List)
**Goal**: High-level status of all import operations.

**Layout:**
- **Header**: Title "Bulk Import Batches", Button "New Import Batch".
- **Filters**: Status (Running, Failed, Completed), Date Range.
- **Table**:
    - **ID**: Batch ID (e.g., `#BATCH-102`)
    - **Source**: Filename (e.g., `Thyagaraja-Krithi.csv`)
    - **Progress**: Multi-segment progress bar (Queued | Scraped | Enriched | Resolved | Completed).
    - **Stats**: `Success / Total` counts.
    - **Status**: Status Chip (Green: Completed, Blue: Running, Red: Failed, Amber: Attention Needed).
    - **Created At**: Timestamp.
    - **Actions**: "View Details", "Pause/Resume" (Quick actions).

**Interactions:**
- Clicking a row navigates to **Batch Detail**.
- Hovering over progress bar shows tooltips with exact counts per stage.

### 4.2. New Batch Wizard
**Goal**: Simple, safe initiation of a new batch.

**UX Flow:**
1.  **Upload**: Drag & drop CSV file.
2.  **Preview**: Show first 5 rows to confirm parsing is correct.
3.  **Config**: Optional settings (e.g., "Skip Scrape" for re-processing, "Priority").
4.  **Confirmation**: "Start Import" button.

### 4.3. Batch Detail View (The "Command Center")
**Goal**: Deep technical monitoring and control.

**Layout:**
- **Header**:
    - Batch ID & Name.
    - **Global Controls**: Big buttons for `PAUSE`, `RESUME`, `CANCEL`, `RETRY FAILED`.
    - **Status Banner**: Prominent status indicator.

- **Section A: Metrics & Health (Top Row)**
    - **Progress**: Circular or Linear progress of the current active job.
    - **Throughput**: Tasks/minute.
    - **Error Rate**: % of tasks failing.
    - **Estimated Time Remaining**.

- **Section B: Job Pipeline (Visual Flow)**
    - A horizontal stepper showing the pipeline stages: `Manifest Ingest` -> `Scrape` -> `Enrich` -> `Resolution` -> `Review` -> `Finalize`.
    - Each step shows its specific status (Pending, Active, Done).

- **Section C: Task Explorer (Main Content)**
    - **Tabs**: `All`, `Failed`, `Blocked/Review`, `Completed`.
    - **Search/Filter**: By URL, Title, or specific Error Message.
    - **Data Grid**:
        - Task ID.
        - Source URL.
        - Stage (Scrape, Enrich, etc.).
        - Status.
        - Duration.
        - Last Error (Truncated).
        - **Action**: "View Log", "Retry Individual", "Resolve" (if blocked).

- **Drawer/Modal**: Clicking a task opens a drawer with:
    - Full Event Log (Timeline).
    - JSON Payload (Input/Output).
    - Full Stack Trace (for errors).
    - Link to "Review Interface" (if data is ready).

### 4.4. Task Review Interface (Entity Resolution & Approval)
**Goal**: Human-in-the-loop decision making.

**Layout:**
- **Split Screen (Diff Viewer)**:
    - **Left**: Scraped Data (New).
    - **Right**: System/Existing Data (or Empty).
    - **Center**: Controls to "Accept Left", "Keep Right", or "Merge".

- **Entity Resolution Mode**:
    - When a Raga/Composer is unmatched.
    - **Prompt**: "We found 'Begada' in CSV. Database has 'Begada (29.1)'. Is this a match?"
    - **Options**:
        - "Yes, Map to ID 123" (High confidence suggestion).
        - "No, Create New Entity".
        - "Search for existing...".

- **Bulk Actions**:
    - Checkbox selection for multiple items.
    - "Approve Selected", "Reject Selected".

## 5. Component Requirements
To implement this, the Frontend Design System needs:
1.  **StatusBadge**: Variants for Orchestration states (Queued, Scraped, Enriched, etc.).
2.  **ProgressBar**: Multi-colored segmented bar.
3.  **LogViewer**: Monospace, collapsible JSON viewer, copy-to-clipboard.
4.  **DiffView**: Visual component to highlight text differences.
5.  **Stepper**: For the Job Pipeline visualization.
6.  **DataGrid**: Virtualized table for high performance (1000+ rows).

## 6. Implementation Phases (Aligned with Backend)
- **Phase 1 (Foundation)**: Batch List, New Batch Wizard, Basic Detail View (Task list + Logs).
- **Phase 2 (Control)**: Pause/Resume/Retry integration, Error Drill-down.
- **Phase 3 (Resolution)**: Entity Resolution UI, Confidence Score indicators.
- **Phase 4 (Workflow)**: Full Review Queue, Bulk Approvals.
