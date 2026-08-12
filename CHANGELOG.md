# Changelog

All notable changes to **TODO++** will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] – 2026-08-13

### 🚀 Issue Tracker REST Integration & Webhooks
- **GitHub Issues REST Export**: Export any TODO task directly as a GitHub issue (`POST /repos/{owner}/{repo}/issues`) via right-click context menu or toolbar action.
- **Jira Cloud REST Export**: Export any TODO task directly to Jira Cloud (`POST /rest/api/3/issue`) with formatted markdown descriptions and project field mapping.
- **Slack & Discord Overdue Webhooks**: 1-click dispatch of formatted Overdue TODO alert notifications directly to Slack (Block Kit) or Discord (Embed) webhook endpoints.
- **Integrations & Webhooks Settings**: New credentials and endpoint settings section in **Settings > Tools > TODO++** for GitHub Token, Jira credentials, and Webhook URLs.

### ⚡ Performance & Scalability (5,000+ Items)
- **Smart Group-Level Tree Expansion**: Replaced full recursive tree expansion with top-level group node expansion when datasets exceed 200 items, keeping UI rendering instantaneous.
- **200ms Live Filter Debouncing**: Integrated `Alarm` debouncing on Search/Assignee/Category fields to buffer live typing input.

---

## [2.2.0] – 2026-08-12

### ⚡ Performance & Scalability Enhancements
- **Native IDE Exclusions (`ProjectFileIndex`)**: Scanning queries IntelliJ's `ProjectFileIndex.isExcluded(file)` to automatically skip files and directories marked as excluded by `.gitignore`, build tools, or project settings
- **Real-Time Scan Progress**: Progress manager now displays percentage fractions and granular file counters (`Scanning file 142 of 1,250: Main.kt`) in the background tasks panel
- **Configurable Max File Size Cap**: Users can now configure maximum file size limits (default 5 MB, adjustable between 1–100 MB) in **Settings > Tools > TODO++**
- **Expanded Default Ignored Directories**: Added `.next`, `.nuxt`, `coverage`, `.venv`, `venv`, `__pycache__`, `.cargo` to default directory exclusion rules
- **Cross-Platform Path Normalization**: Path separator matching automatically normalizes backslashes (`\`) on Windows to ensure directory exclusion rules work seamlessly across all OSs

### 📚 Methodology & Documentation
- **AIDLC (AI-Driven Development Life Cycle)**: Adopted structured AIDLC workflow (Plan -> Implement -> Verify -> Sandbox Test -> Document) with documented guidelines in `docs/AIDLC_GUIDE.md`

---

## [2.1.0] – 2026-07-21

### 🐛 Bug Fixes
- **Block comment TODO detection**: TODOs inside `/* TODO: ... */` (standalone and mid-line) were silently ignored. Fixed by updating the regex prefix from `/\*` → `/\*+` so it matches `/*`, `/**`, and `/***` delimiters, and adding `*/` as a description boundary in the lookahead
- **Javadoc / KDoc detection**: `/** TODO: ... */` style comments now correctly detected
- **Description clean-up**: Trailing `*/` is no longer included in the captured TODO description

### ✨ New Features
- **Customisable HTML Export** (`HtmlExportConfig`): Override any of 5 sections of the exported HTML dashboard — page title, custom CSS (including criticality-level colours), stats counter block (with `{{TOTAL/CRITICAL/HIGH/MEDIUM/LOW}}` tokens), the list/table section (`{{ROWS}}` token), and a custom footer. Config is persisted in IDE settings XML

### 🔧 Improvements
- **Priority group urgency sorting**: When grouped by Priority in the tree, groups now sort `CRITICAL → HIGH → MEDIUM → LOW → None` instead of alphabetically
- **Large file safety guard**: Files over 5 MB are automatically skipped during scanning to prevent `OutOfMemoryError` on generated assets and logs

---

## [2.1.0] – 2026-07-21

### 🐛 Bug Fixes
- **Block comment TODO detection**: TODOs inside `/* TODO: ... */` (standalone and mid-line) were silently ignored. Fixed by updating the regex prefix from `/\*` → `/\*+` so it matches `/*`, `/**`, and `/***` delimiters, and adding `*/` as a description boundary in the lookahead
- **Javadoc / KDoc detection**: `/** TODO: ... */` style comments now correctly detected
- **Description clean-up**: Trailing `*/` is no longer included in the captured TODO description

### ✨ New Features
- **Customisable HTML Export** (`HtmlExportConfig`): Override any of 5 sections of the exported HTML dashboard — page title, custom CSS (including criticality-level colours), stats counter block (with `{{TOTAL/CRITICAL/HIGH/MEDIUM/LOW}}` tokens), the list/table section (`{{ROWS}}` token), and a custom footer. Config is persisted in IDE settings XML

### 🔧 Improvements
- **Priority group urgency sorting**: When grouped by Priority in the tree, groups now sort `CRITICAL → HIGH → MEDIUM → LOW → None` instead of alphabetically
- **Large file safety guard**: Files over 5 MB are automatically skipped during scanning to prevent `OutOfMemoryError` on generated assets and logs

---

## [2.0.0] – 2026-07-20


### ✨ New Features
- **Live Auto-Scan**: TODO list updates automatically within 500ms as you type — no manual refresh needed
- **Quick Fix Intentions**: Mark any TODO complete/incomplete directly from the code editor via Alt+Enter
- **Priority Badges**: Colour-coded badges (🟣 Critical / 🔴 High / 🟠 Medium / 🟢 Low) visible in the tree table
- **Copy for Standup**: One-click copy of selected TODOs formatted as a Markdown/Slack checklist
- **Multi-Line TODO Comments**: Indented bullet points beneath a TODO are automatically captured as multi-line descriptions
- **Custom Keywords**: Define your own patterns (`HACK:`, `BUG:`, `NOTE:`, `OPTIMIZE:`) in Settings
- **Sound Effects**: Optional subtle chime and pop audio feedback on task completion
- **Export to PDF**: Generate printable PDF task reports from the toolbar
- **Full Solution Scope**: Scans `.cshtml`, `.razor`, `.vue`, `.svelte`, `.dart`, `.yaml`, `.json`, `.toml`, `.md`, `.env` and all non-binary registered file types
- **Priority Urgency Sorting**: Tree groups now sort CRITICAL → HIGH → MEDIUM → LOW

### 🐛 Bug Fixes
- Fixed strikethrough styling being lost on selected rows in the tree table
- Fixed `SimpleToolWindowPanel` deprecated constructor warning
- Fixed `ColorChooser` deprecation — replaced with `JColorChooser.showDialog`
- Fixed memory leak: `TodoToolWindowContent` now implements `Disposable` and is bound to tool window lifecycle

### 🔧 Improvements
- Scanner now skips files larger than 5MB to prevent `OutOfMemoryError` on large generated assets
- VCS annotation fetching now runs outside the PSI ReadAction lock, preventing UI thread deadlocks
- Added `bin`, `obj`, `target`, `.gradle`, `vendor` to default ignored directories
- Plugin Verifier: **100% Compatible** against IntelliJ Platform 2024.1+ with zero deprecation warnings

---

## [1.9.0] – 2026-06-01

### ✨ New Features
- Multi-selection with batch complete/incomplete via Green Tick (✔) and Red Cross (✖) toolbar buttons
- Single-line multi-TODO parsing — multiple TODOs on one line now each appear as separate entries
- Expanded file extension support including web frameworks and config formats

### 🐛 Bug Fixes
- Fixed scope label to read "Entire Solution / Project"
- Fixed toolbar icon rendering on macOS

---

## [1.0.0] – 2026-01-01

### 🎉 Initial Release
- Project-wide TODO scanner for IntelliJ-based IDEs
- Tree table grouped by File, Assignee, Priority, or Category
- Settings panel with custom patterns, ignored directories, and colour preferences
- VCS blame integration showing author and commit date per TODO
