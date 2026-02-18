package com.todoplus.models

/**
 * Represents a TODO item with enhanced metadata
 *
 * Example: // TODO(@john priority:high category:bug): Fix memory leak
 */
data class TodoItem(
    val description: String,
    val assignee: String? = null,
    val priority: Priority? = null,
    val category: String? = null,
    val issueId: String? = null,
    val tags: Map<String, String> = emptyMap(),
    val dueDate: java.time.LocalDate? = null,
    val filePath: String,
    val lineNumber: Int,
    val fullText: String
) {
    fun getFileName(): String = filePath.substringAfterLast('/')

    fun getDisplayText(): String {
        val components = mutableListOf<String>()
        if (priority != null) components.add("[$priority]")
        if (issueId != null) components.add("[Issue: $issueId]")
        if (dueDate != null) components.add("[Due: $dueDate]")
        if (category != null) components.add("[$category]")
        // Don't duplicate tags if they are already in properties
        val displayedTags = tags.filterKeys { it != "priority" && it != "category" && it != "due" && it != "issue" }
        if (displayedTags.isNotEmpty()) {
            val tagsStr = displayedTags.entries.joinToString(" ") { "${it.key}:${it.value}" }
            components.add("($tagsStr)")
        }
        components.add(description)
        return components.joinToString(" ")
    }

    fun isPlainTodo(): Boolean {
        return assignee == null && priority == null && category == null && dueDate == null && issueId == null && tags.isEmpty()
    }
}
