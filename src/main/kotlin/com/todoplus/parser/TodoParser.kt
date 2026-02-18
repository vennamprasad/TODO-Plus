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
class TodoParser(private val issuePattern: String = "") {

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

        val metadataStr = matchResult.groupValues[1] // Everything in parentheses
        val description = matchResult.groupValues[2].trim()

        // Extract all metadata
        val (assignee, priority, category, explicitIssueId, dueDate, tags) = extractMetadata(metadataStr)
        
        // If explicit issue ID not found, try to find in description using configured pattern
        var finalIssueId = explicitIssueId
        if (finalIssueId == null && issuePattern.isNotEmpty()) {
             try {
                 val regex = Regex(issuePattern)
                 val match = regex.find(description)
                 if (match != null) {
                     finalIssueId = match.value
                 }
             } catch (e: Exception) {
                 // Invalid regex, ignore
             }
        }

        return TodoItem(
            description = description,
            assignee = assignee,
            priority = priority,
            category = category,
            issueId = finalIssueId,
            tags = tags,
            dueDate = dueDate,
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

    private data class Metadata(
        val assignee: String? = null,
        val priority: Priority? = null,
        val category: String? = null,
        val issueId: String? = null,
        val dueDate: java.time.LocalDate? = null,
        val tags: Map<String, String> = emptyMap()
    )

    /**
     * Extract all metadata components from the metadata string
     */
    private fun extractMetadata(metadataStr: String): Metadata {
        if (metadataStr.isBlank()) return Metadata()

        var assignee: String? = null
        var priority: Priority? = null
        var category: String? = null
        var issueId: String? = null
        var dueDate: java.time.LocalDate? = null
        val tags = mutableMapOf<String, String>()

        // Split by spaces, but respect potential future quoting (simple split for now)
        val parts = metadataStr.split(Regex("\\s+"))

        for (part in parts) {
            when {
                // @assignee
                part.startsWith("@") -> {
                    assignee = part.substring(1)
                }
                // key:value
                part.contains(":") -> {
                    val keyVal = part.split(":", limit = 2)
                    if (keyVal.size == 2) {
                        val key = keyVal[0].lowercase()
                        val value = keyVal[1]

                        when (key) {
                            "priority" -> priority = Priority.parse(value)
                            "category" -> category = value
                            "assignee", "assigned" -> assignee = value
                            "due" -> dueDate = parseDueDate(value)
                            "issue" -> issueId = value
                            else -> tags[key] = value
                        }
                    }
                }
            }
        }

        return Metadata(assignee, priority, category, issueId, dueDate, tags)
    }

    private fun parseDueDate(value: String): java.time.LocalDate? {
        return try {
            when (value.lowercase()) {
                "today" -> java.time.LocalDate.now()
                "tomorrow" -> java.time.LocalDate.now().plusDays(1)
                else -> java.time.LocalDate.parse(value) // Expects YYYY-MM-DD
            }
        } catch (e: Exception) {
            null
        }
    }
}
