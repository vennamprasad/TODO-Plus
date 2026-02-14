# How to Use TODO++ Tool Window

## Opening the Tool Window

Once the plugin is installed, you can access the TODO++ tool window in two ways:

### Method 1: Tool Window Button
1. Look at the **bottom** of your IntelliJ window
2. Click on the **"TODO++"** button in the tool window bar
3. The tool window will slide up showing your TODOs

### Method 2: View Menu
1. Go to **View → Tool Windows → TODO++**
2. The tool window will appear

## What You'll See

The tool window displays a table with the following columns:

| Column | Description | Example |
|--------|-------------|---------|
| **Priority** | HIGH/MEDIUM/LOW or "-" | HIGH |
| **Assignee** | Person assigned (@username) or "-" | @john |
| **Category** | Type of TODO  or "-" | bug, feature, refactor |
| **Description** | The TODO text | Fix memory leak |
| **File** | Source file name | UserService.kt |
| **Line** | Line number in file | 14 |

## Sample Data

The tool window currently shows **sample demo data** to verify the UI works correctly:

```kotlin
// TODO(@john priority:high category:bug): Fix memory leak in authentication
// TODO(@sarah priority:medium category:feature): Add password reset feature
// TODO(@team priority:low category:refactor): Clean up user profile method
// TODO: Add input validation
```

## Next Steps

In the upcoming weeks, we'll add:
- **Real project scanning** - Parse actual files in your project
- **Filters** - Filter by assignee, priority, or category
- **Navigation** - Click a TODO to jump to that line in the code
- **Refresh button** - Re-scan the project for new TODOs
- **Statistics** - Dashboard showing TODO counts and breakdowns

## Testing Right Now

Current version shows the UI working with hardcoded sample data. The parser is ready and tested, but not yet connected to live file scanning.

**What works:**
✅ Tool window appears in IDE
✅ Table displays TODO information
✅ Sample data demonstrates the format

**Coming next:**
⏳ File scanner to find real TODOs
⏳ Click to navigate to code
⏳ Filters and search
