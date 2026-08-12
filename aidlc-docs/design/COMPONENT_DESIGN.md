# AI-DLC Phase 2: Component Detailed Design

**Project**: TODO++  
**Status**: APPROVED  

---

## 🎨 Component Design Specifications

### 1. `TodoScannerService` API

```kotlin
@Service(Service.Level.PROJECT)
class TodoScannerService(private val project: Project) {
    /**
     * Scans project files for TODO items, reporting progress to indicator.
     */
    fun scanProject(indicator: ProgressIndicator? = null): List<TodoItem>

    /**
     * Scans single virtual file for TODO items. Respects maxFileSizeMb cap.
     */
    fun scanFile(file: VirtualFile): List<TodoItem>

    /**
     * Computes statistics break-down by priority and assignment.
     */
    fun getStatistics(todos: List<TodoItem>): TodoStatistics
}
```

#### Exclusions & Path Normalization Logic:
```kotlin
val filteredFiles = virtualFiles.filter { file ->
    if (fileIndex.isExcluded(file)) return@filter false
    val path = file.path.replace('\\', '/')
    !ignoredDirs.any { ignoredDir ->
        path.contains("/$ignoredDir/") || path.endsWith("/$ignoredDir")
    }
}
```

---

### 2. `TodoSettingsService.State` Model

```kotlin
class State {
    var priorities: MutableList<PriorityConfig> = mutableListOf(...)
    var customKeywords: MutableList<CustomKeywordConfig> = mutableListOf(...)
    var enableAudioFeedback: Boolean = true
    var issueUrlTemplate: String = ""
    var issuePattern: String = "[A-Z]+-\\d+"
    var ignoredDirectories: MutableList<String> = mutableListOf(
        "build", "node_modules", ".idea", ".git", "out", "dist", "bin", "obj",
        "target", ".gradle", "vendor", ".next", ".nuxt", "coverage", ".venv", "venv", "__pycache__", ".cargo"
    )
    var maxFileSizeMb: Int = 5
    var completionBehavior: String = BEHAVIOR_MARK_DONE
    var htmlExport: HtmlExportSettings = HtmlExportSettings()

    // Integration & Webhook settings
    var githubToken: String = ""
    var githubRepoOwner: String = ""
    var githubRepoName: String = ""
    var jiraBaseUrl: String = ""
    var jiraEmail: String = ""
    var jiraApiToken: String = ""
    var jiraProjectKey: String = ""
    var slackWebhookUrl: String = ""
    var discordWebhookUrl: String = ""
}
```

---

### 3. `IssueExporterService` API Spec

```kotlin
object IssueExporterService {
    fun createGitHubIssue(todo: TodoItem, token: String, owner: String, repo: String): Result<String>
    fun createJiraIssue(todo: TodoItem, baseUrl: String, email: String, apiToken: String, projectKey: String): Result<String>
}
```

---

### 4. `WebhookNotificationService` API Spec

```kotlin
object WebhookNotificationService {
    fun sendOverdueSlackNotification(webhookUrl: String, overdueTodos: List<TodoItem>): Result<Unit>
    fun sendOverdueDiscordNotification(webhookUrl: String, overdueTodos: List<TodoItem>): Result<Unit>
}
```

---

### 5. Settings UI (`TodoSettingsConfigurable`)

- **Priority Levels Panel**: `JBList` with `ToolbarDecorator` (Add, Remove, Move Up, Move Down, Edit Color).
- **Ignored Directories Panel**: `JBList` for user-defined folder exclusion strings.
- **Issue Tracker Panel**: URL template input field and Regex pattern field.
- **Scanning Performance Limits Panel**: `JSpinner` for `maxFileSizeMb` adjustment (1 MB to 100 MB).
- **Task Completion Action**: Radio buttons selecting `MARK_DONE` vs `DELETE_COMMENT`.
