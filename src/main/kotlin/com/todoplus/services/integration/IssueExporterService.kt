package com.todoplus.services.integration

import com.todoplus.models.TodoItem
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

/**
 * Service to export TODO items directly as issues to GitHub or Jira via REST API
 */
object IssueExporterService {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    /**
     * Create issue on GitHub via REST API
     * POST https://api.github.com/repos/{owner}/{repo}/issues
     */
    fun createGitHubIssue(todo: TodoItem, token: String, owner: String, repo: String): Result<String> {
        if (token.isBlank() || owner.isBlank() || repo.isBlank()) {
            return Result.failure(IllegalArgumentException("GitHub credentials (token, owner, repo) must be configured in Settings."))
        }

        return try {
            val url = "https://api.github.com/repos/$owner/$repo/issues"
            val title = todo.description.substringBefore('\n')
            val body = buildIssueBody(todo)
            val jsonPayload = """
                {
                  "title": ${escapeJsonString(title)},
                  "body": ${escapeJsonString(body)},
                  "labels": ["todo-plus"]
                }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                val htmlUrl = extractJsonField(response.body(), "html_url") ?: "https://github.com/$owner/$repo/issues"
                Result.success(htmlUrl)
            } else {
                Result.failure(RuntimeException("GitHub API returned status ${response.statusCode()}: ${response.body()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create issue on Jira Cloud via REST API
     * POST https://{baseUrl}/rest/api/3/issue
     */
    fun createJiraIssue(todo: TodoItem, baseUrl: String, email: String, apiToken: String, projectKey: String): Result<String> {
        if (baseUrl.isBlank() || email.isBlank() || apiToken.isBlank() || projectKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Jira credentials (baseUrl, email, apiToken, projectKey) must be configured in Settings."))
        }

        return try {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val url = "$cleanBaseUrl/rest/api/3/issue"
            val summary = todo.description.substringBefore('\n')
            val authHeader = "Basic " + Base64.getEncoder().encodeToString("$email:$apiToken".toByteArray())
            val bodyText = buildIssueBody(todo)

            val jsonPayload = """
                {
                  "fields": {
                    "project": { "key": ${escapeJsonString(projectKey)} },
                    "summary": ${escapeJsonString(summary)},
                    "description": {
                      "type": "doc",
                      "version": 1,
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [
                            { "type": "text", "text": ${escapeJsonString(bodyText)} }
                          ]
                        }
                      ]
                    },
                    "issuetype": { "name": "Task" }
                  }
                }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                val key = extractJsonField(response.body(), "key") ?: projectKey
                val issueUrl = "$cleanBaseUrl/browse/$key"
                Result.success(issueUrl)
            } else {
                Result.failure(RuntimeException("Jira API returned status ${response.statusCode()}: ${response.body()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildIssueBody(todo: TodoItem): String {
        val sb = StringBuilder()
        sb.append(todo.description).append("\n\n")
        sb.append("---").append("\n")
        sb.append("📍 **File**: `").append(todo.filePath).append("` (Line ").append(todo.lineNumber).append(")\n")
        if (todo.priority != null) sb.append("🟣 **Priority**: ").append(todo.priority.name).append("\n")
        if (todo.assignee != null) sb.append("👤 **Assignee**: @").append(todo.assignee).append("\n")
        if (todo.category != null) sb.append("🏷️ **Category**: ").append(todo.category).append("\n")
        if (todo.dueDate != null) sb.append("📅 **Due Date**: ").append(todo.dueDate).append("\n")
        sb.append("\n*Generated automatically by TODO++*")
        return sb.toString()
    }

    private fun escapeJsonString(input: String): String {
        val escaped = input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun extractJsonField(json: String, field: String): String? {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(json)?.groupValues?.get(1)
    }
}
