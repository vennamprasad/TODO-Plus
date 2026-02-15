package com.todoplus.exporter

import com.todoplus.models.TodoItem

/**
 * Service to export TODO items to different formats
 */
class TodoExporter {

    /**
     * Export TODOs to CSV format
     */
    fun exportToCsv(todos: List<TodoItem>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Priority,Assignee,Category,Description,File,Line\n")
        
        // Data
        for (todo in todos) {
            sb.append(escapeCsv(todo.priority?.name ?: "")).append(",")
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
        
        // High Priority
        appendMarkdownSection(sb, "🔴 High Priority", byPriority[com.todoplus.models.Priority.HIGH])
        
        // Medium Priority
        appendMarkdownSection(sb, "🟠 Medium Priority", byPriority[com.todoplus.models.Priority.MEDIUM])
        
        // Low Priority
        appendMarkdownSection(sb, "🟢 Low Priority", byPriority[com.todoplus.models.Priority.LOW])
        
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
            
            sb.append("- [ ] $assigneeStr$categoryStr${todo.description} (`${todo.getFileName()}:${todo.lineNumber}`)\n")
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
