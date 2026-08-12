# How to Use TODO++

## 🚀 fast Start

1.  **Open Todo List**: Click **TODO++** at the bottom of the IDE.
2.  **Write a Todo**: In your code, type `// TODO: Fix this later`.
3.  **Scan**: Click the **🔍 Scan** button in the tool window.

---

## 📝 Syntax Guide

TODO++ parses comments in your code. You can add metadata inside parentheses `(...)` after the TODO keyword.

### Basic
```kotlin
// TODO: Simple reminder
```

### Priority, Assignee, Category
Use these standard keys to organize your tasks:
```kotlin
// TODO(priority:high): Critical bug fix
// TODO(@john): Assigned to John
// TODO(category:refactor): Code cleanup
```
*   **Priorities**: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` (or custom defined).
*   **Assignee**: Starts with `@`.
*   **Category**: Any text.

### 📅 Due Dates
Set deadlines for your tasks. Overdue items will be highlighted in **RED**.
```kotlin
// TODO(due:2025-03-20): Release deadline
// TODO(due:today): Must finish today
// TODO(due:tomorrow): Prepare for meeting
```

### 🔗 Issue Linking
Link TODOs to your external issue tracker (Jira, GitHub, etc.).

**Option 1: Explicit Tag**
```kotlin
// TODO(issue:PROJ-123): Fix validation logic
```

**Option 2: Auto-Detection**
If configured, just mention the ID in the description:
```kotlin
// TODO: Fix validation logic (see PROJ-123)
```
*   **Action**: Right-click the TODO in the list and select **Open in Issue Tracker**.
*   **Setup**: Go to **Settings > Tools > TODO++** to configure your URL template (e.g., `https://jira.com/browse/{id}`).

### 🏷️ Custom Tags
Add any custom key-value pair you need.
```kotlin
// TODO(risk:high estimate:4h): Complex refactoring
// TODO(reviewer:@alice type:security): Security audit needed
```

### ⚡ Power User Combos

**Live Templates**
Just type `todo+` and hit `Tab` or `Space` to instantly generate an enriched comment template!

**Quick Fixes (Intention Actions)**
Have a codebase full of old, basic TODOs? Just type `// TODO: fix this`, click on it, press **`Alt + Enter`** (or Option+Enter) and choose **"Upgrade to TODO++ format"**!

**Full Format Example**
Combine everything into a comprehensive task definition:
```kotlin
// TODO(@me priority:high due:today issue:PROJ-101): Fix critical crash
```

---

## ⚙️ Configuration

Access settings via **Settings/Preferences > Tools > TODO++**.

### Custom Priorities
Define your own priority levels and colors!
1.  Open Settings.
2.  Click **+** to add a priority (e.g., "BLOCKER").
3.  Choose a color (e.g., Purple).
4.  Reorder items to define sort order.

### Issue Tracker & REST Export Setup
1.  **Issue URL Template**: Define where link tags `{id}` go.
    *   GitHub Example: `https://github.com/my-org/my-repo/issues/{id}`
    *   Jira Example: `https://mycompany.atlassian.net/browse/{id}`
2.  **Issue ID Pattern**: Regex pattern matching IDs in code comments (e.g. `[A-Z]+-\d+|#\d+`).
3.  **GitHub REST Integration**:
    *   **Personal Access Token**: GitHub PAT token with `repo` scope.
    *   **Repository Owner**: e.g., `my-org`
    *   **Repository Name**: e.g., `my-repo`
4.  **Jira Cloud REST Integration**:
    *   **Jira Base URL**: `https://mycompany.atlassian.net`
    *   **Account Email**: `user@company.com`
    *   **API Token**: Atlassian API token.
    *   **Project Key**: e.g., `PROJ`
5.  **Slack / Discord Webhooks**:
    *   **Slack Webhook URL**: `https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK_URL`
    *   **Discord Webhook URL**: `https://discord.com/api/webhooks/YOUR/DISCORD/WEBHOOK_URL`

### ⚡ Large Project Scanning & Performance Limits
1.  **Ignored Directories**: Add custom folder names (e.g. `dist`, `coverage`, `.venv`) to prevent scanning unwanted directories.
2.  **Native Exclusions**: Folders excluded in your project settings or `.gitignore` are automatically skipped via IntelliJ's `ProjectFileIndex`.
3.  **Max File Size Limit**: Set the maximum file size (default `5 MB`) to prevent scanning massive generated text or log files.
4.  **High-Volume Tree Rendering**: Top-level group expansion and 200ms `Alarm` filter debouncing guarantee zero EDT freezes on 5,000+ TODO items.
5.  **Cross-Platform Normalization**: Handles Windows (`\`) and Unix (`/`) path formats seamlessly.

---

## 🔎 Tool Window Features

*   **Grouping**: Use the **Group By** dropdown to organize your TODOs hierarchically by File, Assignee, Priority, or Category.
*   **Sorting**: Click any column header (Priority, Due Date, etc.) to sort within groups.
*   **Filtering**:
    *   Type `risk:high` in the search bar to see only high-risk items.
    *   Type `@john` to see John's tasks.
*   **Navigation**: Double-click any row to jump directly to that line of code.
*   **Export**: Automatically export your customized and grouped list to CSV, Markdown, or a **Beautiful Interactive HTML Dashboard**.
