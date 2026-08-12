package com.todoplus.services.integration

import com.todoplus.models.Priority
import com.todoplus.models.TodoItem
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class IntegrationServicesTest {

    @Test
    fun testGitHubIssueExportMissingCredentials() {
        val todo = TodoItem(
            description = "Fix memory leak in scanner",
            filePath = "src/main/kotlin/Scanner.kt",
            lineNumber = 42,
            fullText = "// TODO: Fix memory leak in scanner",
            priority = Priority("HIGH"),
            dueDate = LocalDate.now().minusDays(2)
        )

        val result = IssueExporterService.createGitHubIssue(todo, "", "owner", "repo")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testJiraIssueExportMissingCredentials() {
        val todo = TodoItem(
            description = "Fix memory leak in scanner",
            filePath = "src/main/kotlin/Scanner.kt",
            lineNumber = 42,
            fullText = "// TODO: Fix memory leak in scanner"
        )

        val result = IssueExporterService.createJiraIssue(todo, "https://company.atlassian.net", "", "", "PROJ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testSlackWebhookEmptyOverdueList() {
        val result = WebhookNotificationService.sendOverdueSlackNotification("https://hooks.slack.com/services/xxx", emptyList())
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun testDiscordWebhookEmptyOverdueList() {
        val result = WebhookNotificationService.sendOverdueDiscordNotification("https://discord.com/api/webhooks/xxx", emptyList())
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun testSlackWebhookMissingUrl() {
        val todo = TodoItem(
            description = "Overdue task",
            filePath = "src/Main.kt",
            lineNumber = 10,
            fullText = "// TODO: Overdue task",
            dueDate = LocalDate.now().minusDays(5)
        )
        val result = WebhookNotificationService.sendOverdueSlackNotification("", listOf(todo))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
