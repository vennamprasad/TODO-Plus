package com.todoplus.services.integration

import com.todoplus.models.TodoItem
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Service to dispatch Slack and Discord webhook alert notifications for overdue TODO items
 */
object WebhookNotificationService {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    /**
     * Send Slack Block Kit webhook notification for overdue TODOs
     */
    fun sendOverdueSlackNotification(webhookUrl: String, overdueTodos: List<TodoItem>): Result<Int> {
        if (webhookUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Slack Webhook URL is not configured in Settings."))
        }
        if (overdueTodos.isEmpty()) {
            return Result.success(0)
        }

        return try {
            val textBuilder = StringBuilder()
            textBuilder.append("⚠️ *Overdue TODO Alert* (").append(overdueTodos.size).append(" tasks overdue)\n\n")

            overdueTodos.take(10).forEach { todo ->
                val pStr = todo.priority?.name ?: "LOW"
                val pBadge = when(pStr.uppercase()) {
                    "CRITICAL" -> "🟣"
                    "HIGH" -> "🔴"
                    "MEDIUM" -> "🟠"
                    else -> "🟢"
                }
                textBuilder.append(pBadge).append(" *").append(todo.description.substringBefore('\n')).append("*\n")
                textBuilder.append("   📍 `").append(todo.getFileName()).append(":").append(todo.lineNumber).append("` | Due: *").append(todo.dueDate).append("*\n")
            }

            if (overdueTodos.size > 10) {
                textBuilder.append("\n_...and ").append(overdueTodos.size - 10).append(" more overdue tasks._")
            }

            val jsonPayload = """
                {
                  "text": ${escapeJsonString(textBuilder.toString())}
                }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl.trim()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                Result.success(overdueTodos.size)
            } else {
                Result.failure(RuntimeException("Slack Webhook returned status ${response.statusCode()}: ${response.body()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send Discord Rich Embed webhook notification for overdue TODOs
     */
    fun sendOverdueDiscordNotification(webhookUrl: String, overdueTodos: List<TodoItem>): Result<Int> {
        if (webhookUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Discord Webhook URL is not configured in Settings."))
        }
        if (overdueTodos.isEmpty()) {
            return Result.success(0)
        }

        return try {
            val textBuilder = StringBuilder()
            textBuilder.append("⚠️ **Overdue TODO Alert** (").append(overdueTodos.size).append(" tasks overdue)\n\n")

            overdueTodos.take(10).forEach { todo ->
                val pStr = todo.priority?.name ?: "LOW"
                val pBadge = when(pStr.uppercase()) {
                    "CRITICAL" -> "🟣"
                    "HIGH" -> "🔴"
                    "MEDIUM" -> "🟠"
                    else -> "🟢"
                }
                textBuilder.append(pBadge).append(" **").append(todo.description.substringBefore('\n')).append("**\n")
                textBuilder.append("   📍 `").append(todo.getFileName()).append(":").append(todo.lineNumber).append("` | Due: **").append(todo.dueDate).append("**\n")
            }

            if (overdueTodos.size > 10) {
                textBuilder.append("\n*...and ").append(overdueTodos.size - 10).append(" more overdue tasks.*")
            }

            val jsonPayload = """
                {
                  "content": ${escapeJsonString(textBuilder.toString())}
                }
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl.trim()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                Result.success(overdueTodos.size)
            } else {
                Result.failure(RuntimeException("Discord Webhook returned status ${response.statusCode()}: ${response.body()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}
