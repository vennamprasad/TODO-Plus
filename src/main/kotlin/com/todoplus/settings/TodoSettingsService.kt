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

    class State {
        var priorities: MutableList<PriorityConfig> = mutableListOf(
            PriorityConfig("HIGH", Color(220, 50, 50).rgb),    // Red
            PriorityConfig("MEDIUM", Color(220, 160, 30).rgb),  // Orange
            PriorityConfig("LOW", Color(80, 160, 80).rgb)       // Green
        )
        
        var issueUrlTemplate: String = "" // e.g., https://github.com/user/repo/issues/{id}
        var issuePattern: String = "[A-Z]+-\\d+" // Default: Jira-style (PROJ-123)
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getPriorities(): List<PriorityConfig> = myState.priorities
    
    fun getPriorityColor(name: String): Color? {
        return myState.priorities.find { it.name.equals(name, ignoreCase = true) }?.getColor()
    }
    
    fun setPriorities(newPriorities: List<PriorityConfig>) {
        myState.priorities = newPriorities.toMutableList()
    }

    companion object {
        fun getInstance(): TodoSettingsService = service()
    }
}
