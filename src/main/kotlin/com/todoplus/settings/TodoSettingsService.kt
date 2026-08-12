package com.todoplus.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

/**
 * Persists TODO++ configuration settings (Priorities, Issue Tracker, etc.)
 */
@State(
    name = "com.todoplus.settings.TodoSettingsService",
    storages = [Storage("todoPlus_settings.xml")] // Migrating to new file for clear separation
)
class TodoSettingsService : PersistentStateComponent<TodoSettingsService.State> {

    data class PriorityConfig(
        var name: String = "",
        var colorRgb: Int = 0
    ) {
        constructor() : this("", 0)
        
        fun getColor(): Color = Color(colorRgb)
    }

    data class CustomKeywordConfig(
        var keyword: String = "",
        var colorRgb: Int = 0,
        var iconSymbol: String = "🏷️"
    ) {
        constructor() : this("", 0, "🏷️")
        fun getColor(): Color = Color(colorRgb)
    }

    /**
     * Persisted HTML export customisation fields.
     * All fields default to blank which makes [TodoExporter] use its built-in output.
     */
    data class HtmlExportSettings(
        var pageTitle: String = "",
        var customCss: String = "",
        var statsHtmlOverride: String = "",
        var listHtmlOverride: String = "",
        var footerHtml: String = ""
    ) {
        constructor() : this("", "", "", "", "")
    }

    class State {
        var priorities: MutableList<PriorityConfig> = mutableListOf(
            PriorityConfig("CRITICAL", Color(180, 40, 180).rgb), // Purple
            PriorityConfig("HIGH", Color(220, 50, 50).rgb),    // Red
            PriorityConfig("MEDIUM", Color(220, 160, 30).rgb),  // Orange
            PriorityConfig("LOW", Color(80, 160, 80).rgb)       // Green
        )
        
        var customKeywords: MutableList<CustomKeywordConfig> = mutableListOf(
            CustomKeywordConfig("HACK", Color(200, 100, 40).rgb, "⚡"),
            CustomKeywordConfig("BUG", Color(220, 50, 50).rgb, "🐛"),
            CustomKeywordConfig("NOTE", Color(40, 140, 200).rgb, "📌"),
            CustomKeywordConfig("OPTIMIZE", Color(140, 80, 200).rgb, "🚀")
        )
        
        var enableAudioFeedback: Boolean = true
        var issueUrlTemplate: String = "https://github.com/my-org/my-repo/issues/{id}" // e.g. https://github.com/my-org/my-repo/issues/{id} or https://mycompany.atlassian.net/browse/{id}
        var issuePattern: String = "[A-Z]+-\\d+|#\\d+" // Default: Jira-style (PROJ-123) or GitHub-style (#123)
        var ignoredDirectories: MutableList<String> = mutableListOf(
            "build", "node_modules", ".idea", ".git", "out", "dist", "bin", "obj",
            "target", ".gradle", "vendor", ".next", ".nuxt", "coverage", ".venv", "venv", "__pycache__", ".cargo"
        )
        var maxFileSizeMb: Int = 5
        var completionBehavior: String = BEHAVIOR_MARK_DONE
        var htmlExport: HtmlExportSettings = HtmlExportSettings()

        // GitHub REST Integration
        var githubToken: String = ""
        var githubRepoOwner: String = "my-org"
        var githubRepoName: String = "my-repo"

        // Jira REST Integration
        var jiraBaseUrl: String = "https://mycompany.atlassian.net"
        var jiraEmail: String = "user@company.com"
        var jiraApiToken: String = ""
        var jiraProjectKey: String = "PROJ"

        // Webhook Integration
        var slackWebhookUrl: String = "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK_URL"
        var discordWebhookUrl: String = "https://discord.com/api/webhooks/YOUR/DISCORD/WEBHOOK_URL"
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
        
        // Auto-migrate: Ensure CRITICAL exists for users loading an older settings XML file
        if (myState.priorities.none { it.name.equals("CRITICAL", ignoreCase = true) }) {
            myState.priorities.add(0, PriorityConfig("CRITICAL", Color(180, 40, 180).rgb))
        }
    }

    fun getPriorities(): List<PriorityConfig> = myState.priorities
    
    fun getPriorityColor(name: String): Color? {
        return myState.priorities.find { it.name.equals(name, ignoreCase = true) }?.getColor()
    }
    
    fun setPriorities(newPriorities: List<PriorityConfig>) {
        myState.priorities = newPriorities.toMutableList()
    }
    
    fun getIgnoredDirectories(): List<String> = myState.ignoredDirectories
    
    fun setIgnoredDirectories(dirs: List<String>) {
        myState.ignoredDirectories = dirs.toMutableList()
    }

    fun getHtmlExportConfig(): com.todoplus.exporter.HtmlExportConfig {
        val s = myState.htmlExport
        return com.todoplus.exporter.HtmlExportConfig(
            pageTitle          = s.pageTitle,
            customCss          = s.customCss,
            statsHtmlOverride  = s.statsHtmlOverride,
            listHtmlOverride   = s.listHtmlOverride,
            footerHtml         = s.footerHtml
        )
    }

    fun setHtmlExportConfig(config: com.todoplus.exporter.HtmlExportConfig) {
        myState.htmlExport = HtmlExportSettings(
            pageTitle          = config.pageTitle,
            customCss          = config.customCss,
            statsHtmlOverride  = config.statsHtmlOverride,
            listHtmlOverride   = config.listHtmlOverride,
            footerHtml         = config.footerHtml
        )
    }

    companion object {
        const val BEHAVIOR_MARK_DONE = "MARK_DONE"
        const val BEHAVIOR_DELETE_COMMENT = "DELETE_COMMENT"

        fun getInstance(): TodoSettingsService = service()
    }
}
