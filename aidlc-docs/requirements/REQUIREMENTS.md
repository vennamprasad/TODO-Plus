# AI-DLC Phase 1: Product & Technical Requirements

**Project**: TODO++ (IntelliJ Platform Plugin)  
**Version**: 2.2.0  
**Status**: ACTIVE  

---

## 🎯 Executive Summary
TODO++ is an enhanced TODO management plugin for IntelliJ-based IDEs (IntelliJ IDEA, PyCharm, WebStorm, Rider, GoLand, etc.). It parses, organizes, filters, exports, and tracks TODO items across single files or whole projects.

---

## 📋 Functional Requirements

### 1. Comment Parsing & Metadata Extraction
- **FR-01**: Parse single-line (`//`, `#`, `--`, `;`) and multi-line (`/* ... */`, `/** ... */`) TODO comments across 15+ programming languages.
- **FR-02**: Extract metadata syntax inside `(...)`: `@assignee`, `priority:CRITICAL|HIGH|MEDIUM|LOW`, `due:YYYY-MM-DD`, `category:text`, `issue:ID`, and custom key-value pairs (`key:value`).
- **FR-03**: Support multi-line continuation comments with indented bullet points.

### 2. Project Scanning & Scope Management
- **FR-04**: Support scanning scope selection ("Current File" vs "Entire Solution / Project").
- **FR-05**: Perform background asynchronous scans without freezing the IDE UI thread.
- **FR-06**: Filter out binary files, unindexed files, and user-ignored directory patterns.
- **FR-07**: Utilize native `ProjectFileIndex` to skip excluded files (`.gitignore`, build output folders).
- **FR-08**: Stream granular progress updates (`Scanning file X of Y: filename`, fraction percentage) to the IDE background task indicator.
- **FR-09**: Support configurable file size cap (`maxFileSizeMb`, default 5 MB).

### 3. Tool Window & UI Management
- **FR-10**: Hierarchical tree table with dynamic grouping by File, Assignee, Priority, or Category.
- **FR-11**: Priority badges (🟣 Critical, 🔴 High, 🟠 Medium, 🟢 Low) and deadline color alerts (Red = Overdue, Orange = Due Soon).
- **FR-12**: Multi-selection and batch mark complete/incomplete actions.
- **FR-13**: Jump-to-source on double-click.

### 4. Issue Tracker & VCS Blame Integration
- **FR-14**: Git blame integration fetching author and commit date per TODO item.
- **FR-15**: Issue tracker linking using custom URL templates (e.g. `https://jira.org/browse/{id}`).

### 5. Exporting & Reporting
- **FR-16**: Standalone interactive HTML dashboard generation with custom CSS and statistics overrides.
- **FR-17**: Printable PDF report export.
- **FR-18**: 1-click Markdown / Slack checklist export for standups.

### 6. REST API Integrations & Webhooks
- **FR-19 (Issue REST Export)**: Export selected TODO tasks directly as new issues to GitHub (`POST /repos/{owner}/{repo}/issues`) or Jira (`POST /rest/api/3/issue`) via authenticated REST APIs.
- **FR-20 (Webhook Alerts)**: Send formatted Overdue TODO alert notifications directly to Slack or Discord webhook endpoints.

---

## ⚙️ Non-Functional Requirements

- **NFR-01 (Performance)**: Full project scan of 1,000+ files must complete in under 5 seconds on standard developer machines without UI lag.
- **NFR-02 (Compatibility)**: 100% binary & API compatibility with IntelliJ Platform SDK 2024.1+ (Build `241+`).
- **NFR-03 (Thread Safety)**: Zero synchronous lock contention or EDT blocking. All PSI/VFS access wrapped in `runReadAction`.
- **NFR-04 (Cross-Platform)**: Path separator handling must work seamlessly across macOS, Linux, and Windows (`\` vs `/`).
- **NFR-05 (Rendering Scalability)**: Tree Table rendering and filtering must complete in under 50ms on datasets of 5,000+ items without locking the Event Dispatch Thread (EDT).
