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
    val filePath: String,
    val lineNumber: Int,
    val fullText: String
) {
    /**
     * Returns a user-friendly display string
     */
    fun getDisplayText(): String {
        val parts = mutableListOf<String>()
        
        if (assignee != null) {
            parts.add("@$assignee")
        }
        if (priority != null) {
            parts.add("priority:${priority.name.lowercase()}")
        }
        if (category != null) {
            parts.add("category:$category")
        }
        
        val metadata = if (parts.isNotEmpty()) "(${parts.joinToString(" ")})" else ""
        return "TODO$metadata: $description"
    }

    /**
     * Returns true if this TODO has no metadata (regular TODO)
     */
    fun isPlainTodo(): Boolean {
        return assignee == null && priority == null && category == null
    }

    /**
     * Returns the file name without the full path
     */
    fun getFileName(): String {
        return filePath.substringAfterLast('/')
    }
}
