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
*   **Priorities**: `HIGH`, `MEDIUM`, `LOW` (or custom defined).
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

### ⚡ Power User Combo
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

### Issue Tracker Setup
1.  **Issue URL Template**: Define where links go.
    *   Example: `https://github.com/myuser/myrepo/issues/{id}`
2.  **Issue ID Pattern**: Regex to find IDs.
    *   Default: `[A-Z]+-\d+` (Matches `PROJ-123`).

---

## 🔎 Tool Window Features

*   **Sorting**: Click any column header (Priority, Due Date, etc.) to sort.
*   **Filtering**:
    *   Type `risk:high` in the search bar to see only high-risk items.
    *   Type `@john` to see John's tasks.
*   **Navigation**: Double-click any row to jump to the code.
*   **Export**: Click the Export button to save your list as CSV or Markdown.
