package com.todoplus.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.todoplus.models.Priority
import com.todoplus.models.TodoItem
import com.todoplus.services.TodoScannerService
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * Content panel for the TODO++ tool window
 */
class TodoToolWindowContent(private val project: Project) {

    private val tableModel = DefaultTableModel()
    private val table: JBTable
    private val mainPanel: JBPanel<JBPanel<*>>
    private val statusLabel: JBLabel
    private val allTodos = mutableListOf<TodoItem>()
    private val filteredTodos = mutableListOf<TodoItem>()
    
    // Filter controls
    private val priorityFilter = JComboBox(arrayOf("All Priorities", "HIGH", "MEDIUM", "LOW", "None"))
    private val assigneeFilter = JBTextField()
    private val categoryFilter = JBTextField()
    private val searchField = JBTextField()

    init {
        // Initialize table with columns
        tableModel.addColumn("Priority")
        tableModel.addColumn("Assignee")
        tableModel.addColumn("Category")
        tableModel.addColumn("Description")
        tableModel.addColumn("File")
        tableModel.addColumn("Line")

        table = JBTable(tableModel).apply {
            // Make table read-only
            setDefaultEditor(Any::class.java, null)
            // Enable row selection
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            
            // Color-code priorities
            setDefaultRenderer(Any::class.java, PriorityColorRenderer())
            
            // Add click listener for navigation
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        navigateToSelectedTodo()
                    }
                }
            })
        }

        // Create filter panel
        val filterPanel = createFilterPanel()

        // Create toolbar with buttons
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            val refreshButton = JButton("🔄 Refresh").apply {
                toolTipText = "Refresh TODO list"
                addActionListener { refreshTodos() }
            }
            add(refreshButton)
            
            val scanButton = JButton("🔍 Scan Project").apply {
                toolTipText = "Scan entire project for TODOs"
                addActionListener { scanProject() }
            }
            add(scanButton)
            
            val clearFiltersButton = JButton("✖ Clear Filters").apply {
                toolTipText = "Clear all filters"
                addActionListener { clearFilters() }
            }
            add(clearFiltersButton)
        }

        statusLabel = JBLabel("Ready. Click 'Scan Project' to find TODOs.")
        statusLabel.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)

        // Create main panel
        mainPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val header = JBLabel("TODO++ - Enhanced TODO Management")
            header.border = BorderFactory.createEmptyBorder(10, 10, 5, 10)
            header.font = header.font.deriveFont(Font.BOLD, 14f)
            
            val topPanel = JPanel(BorderLayout()).apply {
                add(header, BorderLayout.WEST)
                add(toolbar, BorderLayout.EAST)
            }
            
            add(topPanel, BorderLayout.NORTH)
            add(filterPanel, BorderLayout.AFTER_LINE_ENDS)
            add(JBScrollPane(table), BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
    }

    private fun createFilterPanel(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Filters")
        }

        // Priority filter
        panel.add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Priority:"))
            priorityFilter.addActionListener { applyFilters() }
            add(priorityFilter)
        })

        // Assignee filter
        panel.add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Assignee:"))
            assigneeFilter.columns = 10
            assigneeFilter.toolTipText = "Filter by assignee (e.g., @john)"
            val filterButton = JButton("Apply").apply {
                addActionListener { applyFilters() }
            }
            add(assigneeFilter)
            add(filterButton)
        })

        // Category filter
        panel.add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Category:"))
            categoryFilter.columns = 10
            categoryFilter.toolTipText = "Filter by category (e.g., bug, feature)"
            val filterButton = JButton("Apply").apply {
                addActionListener { applyFilters() }
            }
            add(categoryFilter)
            add(filterButton)
        })

        // Search field
        panel.add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Search:"))
            searchField.columns = 10
            searchField.toolTipText = "Search in description"
            val searchButton = JButton("Search").apply {
                addActionListener { applyFilters() }
            }
            add(searchField)
            add(searchButton)
        })

        return panel
    }

    fun getContent(): JComponent = mainPanel

    private fun scanProject() {
        statusLabel.text = "Scanning project..."
        allTodos.clear()
        
        try {
            val scanner = project.service<TodoScannerService>()
            val foundTodos = scanner.scanProject()
            allTodos.addAll(foundTodos)
            
            applyFilters()
            
            updateStatistics()
        } catch (e: Exception) {
            statusLabel.text = "Error scanning project: ${e.message}"
        }
    }

    private fun applyFilters() {
        filteredTodos.clear()
        tableModel.rowCount = 0
        
        val priorityFilterValue = priorityFilter.selectedItem as String
        val assigneeText = assigneeFilter.text.trim().removePrefix("@").lowercase()
        val categoryText = categoryFilter.text.trim().lowercase()
        val searchText = searchField.text.trim().lowercase()
        
        allTodos.forEach { todo ->
            var matches = true
            
            // Priority filter
            if (priorityFilterValue != "All Priorities") {
                matches = when (priorityFilterValue) {
                    "None" -> todo.priority == null
                    else -> todo.priority?.name == priorityFilterValue
                }
            }
            
            // Assignee filter
            if (matches && assigneeText.isNotEmpty()) {
                matches = todo.assignee?.lowercase()?.contains(assigneeText) == true
            }
            
            // Category filter
            if (matches && categoryText.isNotEmpty()) {
                matches = todo.category?.lowercase()?.contains(categoryText) == true
            }
            
            // Search filter
            if (matches && searchText.isNotEmpty()) {
                matches = todo.description.lowercase().contains(searchText)
            }
            
            if (matches) {
                filteredTodos.add(todo)
                addTodoToTable(todo)
            }
        }
        
        if (allTodos.isNotEmpty()) {
            updateStatistics()
        } else {
            statusLabel.text = "Showing ${filteredTodos.size} of ${allTodos.size} TODOs"
        }
    }
    
    private fun updateStatistics() {
        val scanner = project.service<TodoScannerService>()
        val stats = scanner.getStatistics(allTodos)
        
        // Count TODOs missing metadata
        val missingPriority = allTodos.count { it.priority == null }
        val missingAssignee = allTodos.count { it.assignee == null }
        
        val statusParts = mutableListOf<String>()
        
        // Main stats
        if (filteredTodos.size < allTodos.size) {
            statusParts.add("Showing ${filteredTodos.size} of ${stats.total} TODOs")
        } else {
            statusParts.add("Found ${stats.total} TODOs")
        }
        
        // Priority breakdown
        val priorityBreakdown = buildString {
            append("(")
            val parts = mutableListOf<String>()
            if (stats.highPriority > 0) parts.add("${stats.highPriority} high")
            if (stats.mediumPriority > 0) parts.add("${stats.mediumPriority} medium")
            if (stats.lowPriority > 0) parts.add("${stats.lowPriority} low")
            append(parts.joinToString(", "))
            append(")")
        }
        if (stats.highPriority + stats.mediumPriority + stats.lowPriority > 0) {
            statusParts.add(priorityBreakdown)
        }
        
        // Missing metadata alerts
        val alerts = mutableListOf<String>()
        if (missingPriority > 0) alerts.add("$missingPriority need priority")
        if (missingAssignee > 0) alerts.add("$missingAssignee unassigned")
        
        if (alerts.isNotEmpty()) {
            statusParts.add("⚠️ ${alerts.joinToString(", ")}")
        }
        
        statusLabel.text = statusParts.joinToString(" | ")
    }

    private fun clearFilters() {
        priorityFilter.selectedIndex = 0
        assigneeFilter.text = ""
        categoryFilter.text = ""
        searchField.text = ""
        applyFilters()
    }

    private fun refreshTodos() {
        scanProject()
    }

    private fun navigateToSelectedTodo() {
        val selectedRow = table.selectedRow
        if (selectedRow < 0 || selectedRow >= filteredTodos.size) return
        
        val todo = filteredTodos[selectedRow]
        val file = LocalFileSystem.getInstance().findFileByPath(todo.filePath) ?: return
        val descriptor = OpenFileDescriptor(project, file, todo.lineNumber - 1, 0)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    private fun addTodoToTable(todo: TodoItem) {
        tableModel.addRow(
            arrayOf(
                todo.priority?.name ?: "-",
                todo.assignee ?: "-",
                todo.category ?: "-",
                todo.description,
                todo.getFileName(),
                todo.lineNumber
            )
        )
    }

    /**
     * Custom cell renderer for color-coding priorities and highlighting missing metadata
     */
    private class PriorityColorRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (!isSelected) {
                when (column) {
                    0 -> {
                        // Priority column - color-coded
                        component.foreground = when (value?.toString()) {
                            "HIGH" -> Color(220, 50, 50)    // Red
                            "MEDIUM" -> Color(220, 160, 30)  // Orange
                            "LOW" -> Color(80, 160, 80)      // Green
                            else -> {
                                // No priority - use italic gray to indicate it needs attention
                                component.font = component.font.deriveFont(Font.ITALIC)
                                Color(150, 150, 150)
                            }
                        }
                        if (value?.toString() != "-") {
                            component.font = component.font.deriveFont(Font.BOLD)
                        }
                    }
                    1, 2 -> {
                        // Assignee and Category columns - gray out if empty
                        if (value?.toString() == "-") {
                            component.foreground = Color(150, 150, 150)
                            component.font = component.font.deriveFont(Font.ITALIC)
                        } else {
                            component.foreground = table.foreground
                        }
                    }
                    else -> {
                        component.foreground = table.foreground
                    }
                }
            }
            
            return component
        }
    }
}
