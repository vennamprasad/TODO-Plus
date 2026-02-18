package com.todoplus.exporter

import com.todoplus.models.TodoItem

/**
 * Service to export TODO items to different formats
 */
class TodoExporter {

    /**
     * Export TODOs to CSV format
     */
    /**
     * Export TODOs to CSV format
     */
    fun exportToCsv(todos: List<TodoItem>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Priority,Due Date,Assignee,Category,Description,File,Line\n")
        
        // Data
        for (todo in todos) {
            sb.append(escapeCsv(todo.priority?.name ?: "")).append(",")
            sb.append(escapeCsv(todo.dueDate?.toString() ?: "")).append(",")
            sb.append(escapeCsv(todo.assignee ?: "")).append(",")
            sb.append(escapeCsv(todo.category ?: "")).append(",")
            sb.append(escapeCsv(todo.description)).append(",")
            sb.append(escapeCsv(todo.filePath)).append(",")
            sb.append(todo.lineNumber).append("\n")
        }
        
        return sb.toString()
    }

    /**
     * Export TODOs to Markdown format
     */
    fun exportToMarkdown(todos: List<TodoItem>): String {
        val sb = StringBuilder()
        sb.append("# TODO List Export\n\n")
        
        // Group by priority
        val byPriority = todos.groupBy { it.priority }
        
        // Get sorted priorities from settings to ensure order
        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        val sortedPriorities = settings.getPriorities().map { com.todoplus.models.Priority(it.name) }
        
        // Export known priorities in order
        for (priority in sortedPriorities) {
            val items = byPriority[priority]
            if (!items.isNullOrEmpty()) {
                val pName = priority.name
                val icon = when(pName.uppercase()) {
                    "HIGH", "CRITICAL" -> "🔴"
                    "MEDIUM" -> "🟠"
                    "LOW" -> "🟢"
                    else -> "⚪"
                }
                appendMarkdownSection(sb, "$icon $pName Priority", items)
            }
        }
        
        // Export priorities NOT in settings (e.g. if settings changed but todos exist)
        val unknownPriorities = byPriority.keys.filterNotNull().filter { it !in sortedPriorities }
        for (priority in unknownPriorities) {
             appendMarkdownSection(sb, "⚪ ${priority.name} Priority", byPriority[priority])
        }
        
        // No Priority
        appendMarkdownSection(sb, "⚪ No Priority", byPriority[null])
        
        return sb.toString()
    }
    
    private fun appendMarkdownSection(sb: StringBuilder, title: String, items: List<TodoItem>?) {
        if (items.isNullOrEmpty()) return
        
        sb.append("## $title\n\n")
        for (todo in items) {
            val assigneeStr = if (todo.assignee != null) "**@${todo.assignee}** " else ""
            val categoryStr = if (todo.category != null) "[${todo.category}] " else ""
            val dueStr = if (todo.dueDate != null) "📅 ${todo.dueDate} " else ""
            
            sb.append("- [ ] $dueStr$assigneeStr$categoryStr${todo.description} (`${todo.getFileName()}:${todo.lineNumber}`)\n")
        }
        sb.append("\n")
    }
    
    private fun escapeCsv(value: String): String {
        var result = value
        if (result.contains(",") || result.contains("\"") || result.contains("\n")) {
            result = result.replace("\"", "\"\"")
            result = "\"$result\""
        }
        return result
    }
}
