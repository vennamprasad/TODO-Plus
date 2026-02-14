package com.todoplus.parser

import com.todoplus.models.Priority
import com.todoplus.models.TodoItem

/**
 * Parser for extracting enhanced TODO comments from code
 * 
 * Supports formats:
 * - // TODO: Simple todo
 * - // TODO(@john): Todo with assignee
 * - // TODO(priority:high): Todo with priority
 * - // TODO(category:bug): Todo with category
 * - // TODO(@john priority:high category:bug): Full format
 */
class TodoParser {

    companion object {
        // Regex pattern to match TODO comments with optional metadata
        // Matches: // TODO(@assignee priority:level category:type): Description
        private val TODO_PATTERN = Regex(
            """//\s*TODO\s*(?:\((.*?)\))?\s*:\s*(.+)""",
            RegexOption.IGNORE_CASE
        )

        // Pattern to extract assignee: @username
        private val ASSIGNEE_PATTERN = Regex("""@(\w+)""")

        // Pattern to extract priority: priority:level
        private val PRIORITY_PATTERN = Regex("""priority:(\w+)""", RegexOption.IGNORE_CASE)

        // Pattern to extract category: category:type
        private val CATEGORY_PATTERN = Regex("""category:(\w+)""", RegexOption.IGNORE_CASE)
    }

    /**
     * Parse a single line of code for TODO comments
     * 
     * @param line The line of code to parse
     * @param filePath The path to the file containing this line
     * @param lineNumber The line number (1-indexed)
     * @return TodoItem if a TODO is found, null otherwise
     */
    fun parseLine(line: String, filePath: String, lineNumber: Int): TodoItem? {
        val matchResult = TODO_PATTERN.find(line) ?: return null

        val metadata = matchResult.groupValues[1] // Everything in parentheses
        val description = matchResult.groupValues[2].trim()

        // Extract metadata components
        val assignee = extractAssignee(metadata)
        val priority = extractPriority(metadata)
        val category = extractCategory(metadata)

        return TodoItem(
            description = description,
            assignee = assignee,
            priority = priority,
            category = category,
            filePath = filePath,
            lineNumber = lineNumber,
            fullText = line.trim()
        )
    }

    /**
     * Parse multiple lines from a file
     * 
     * @param lines The lines to parse
     * @param filePath The path to the file
     * @return List of TodoItems found
     */
    fun parseLines(lines: List<String>, filePath: String): List<TodoItem> {
        return lines.mapIndexedNotNull { index, line ->
            parseLine(line, filePath, index + 1)
        }
    }

    /**
     * Extract assignee from metadata string
     */
    private fun extractAssignee(metadata: String): String? {
        val match = ASSIGNEE_PATTERN.find(metadata) ?: return null
        return match.groupValues[1]
    }

    /**
     * Extract priority from metadata string
     */
    private fun extractPriority(metadata: String): Priority? {
        val match = PRIORITY_PATTERN.find(metadata) ?: return null
        val priorityStr = match.groupValues[1]
        return Priority.parse(priorityStr)
    }

    /**
     * Extract category from metadata string
     */
    private fun extractCategory(metadata: String): String? {
        val match = CATEGORY_PATTERN.find(metadata) ?: return null
        return match.groupValues[1]
    }
}
