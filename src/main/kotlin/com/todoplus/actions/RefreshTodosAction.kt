package com.todoplus.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import com.todoplus.services.TodoScannerService

/**
 * Action to refresh/scan project for TODOs
 */
class RefreshTodosAction : AnAction("Refresh TODOs", "Scan project for TODO items", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Get the scanner service
        val scanner = project.service<TodoScannerService>()
        
        // Scan project
        val todos = scanner.scanProject()
        
        // Update tool window
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("TODO++") ?: return
        
        // Get the content and update it
        val content = toolWindow.contentManager.contents.firstOrNull() ?: return
        val component = content.component
        
        // Find TodoToolWindowContent and update
        updateToolWindow(component, todos)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun updateToolWindow(component: Any, todos: List<com.todoplus.models.TodoItem>) {
        // This is a simplified approach - in production you'd use a proper event system
        // For now, we'll need to add a method to refresh the tool window
    }
}
