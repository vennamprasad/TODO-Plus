package com.todoplus.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.Gray
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.todoplus.models.TodoItem
import com.todoplus.services.TodoScannerService
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.todoplus.exporter.TodoExporter
import java.io.File
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import com.intellij.openapi.application.ApplicationManager
import javax.swing.Timer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator

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
            
            // Enable column sorting
            autoCreateRowSorter = true
            
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
        
        // Configure custom sorting for priority column
        configureSorting()

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
            
            val exportCsvButton = JButton("📄 Export CSV").apply {
                toolTipText = "Export TODOs to CSV file"
                addActionListener { exportTodos("csv") }
            }
            add(exportCsvButton)
            
            val exportMdButton = JButton("📋 Export Markdown").apply {
                toolTipText = "Export TODOs to Markdown file"
                addActionListener { exportTodos("md") }
            }
            add(exportMdButton)
            
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
        
        // Auto-refresh on file save
        setupAutoRefresh()
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
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning for TODOs", true) {
            override fun run(indicator: ProgressIndicator) {
                // Background thread
                indicator.text = "Scanning project files..."
                indicator.isIndeterminate = true
                
                try {
                    val scanner = project.service<TodoScannerService>()
                    // Run scanning (service handles Read Actions internally)
                    val foundTodos = scanner.scanProject()
                    
                    // Update UI on EDT
                    ApplicationManager.getApplication().invokeLater {
                        allTodos.clear()
                        allTodos.addAll(foundTodos)
                        
                        applyFilters()
                        updateStatistics()
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "Error scanning project: ${e.message}"
                    }
                }
            }
        })
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
                tableModel.addRow(arrayOf(
                    todo.priority?.name ?: "-",
                    if (todo.assignee != null) "@${todo.assignee}" else "-",
                    todo.category ?: "-",
                    todo.description,
                    todo.getFileName(),
                    todo.lineNumber
                ))
            }
        }
        
        updateStatistics()
    }
    
    private fun updateStatistics() {
        val count = filteredTodos.size
        val highPriority = filteredTodos.count { it.priority?.name == "HIGH" }
        val mediumPriority = filteredTodos.count { it.priority?.name == "MEDIUM" }
        val lowPriority = filteredTodos.count { it.priority?.name == "LOW" }
        
        val missingPriority = filteredTodos.count { it.priority == null }
        val missingAssignee = filteredTodos.count { it.assignee == null }
        
        val sb = StringBuilder("Found $count TODOs")
        if (count > 0) {
            sb.append(" ($highPriority high, $mediumPriority medium, $lowPriority low)")
            
            if (missingPriority > 0 || missingAssignee > 0) {
                sb.append(" | ⚠️ ")
                val warnings = mutableListOf<String>()
                if (missingPriority > 0) warnings.add("$missingPriority need priority")
                if (missingAssignee > 0) warnings.add("$missingAssignee unassigned")
                sb.append(warnings.joinToString(", "))
            }
        }
        
        statusLabel.text = sb.toString()
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
        if (selectedRow != -1) {
            // Convert view row index to model row index in case of sorting
            val modelRow = table.convertRowIndexToModel(selectedRow)
            val todo = filteredTodos[modelRow]
            
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(todo.filePath)
            if (virtualFile != null) {
                // Navigate to file and line
                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(project, virtualFile, todo.lineNumber - 1, 0),
                    true
                )
            }
        }
    }
    
    /**
     * Custom cell renderer for priority column
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
                                Gray._150
                            }
                        }
                        if (value?.toString() != "-") {
                            component.font = component.font.deriveFont(Font.BOLD)
                        }
                    }
                    1, 2 -> {
                        // Assignee and Category columns - gray out if empty
                        if (value?.toString() == "-") {
                            component.foreground = Gray._150
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
    
    /**
     * Configure custom sorting for table columns
     */
    private fun configureSorting() {
        val sorter = table.rowSorter as? javax.swing.table.TableRowSorter<*> ?: return
        
        // Column 0: Priority - Custom comparator (HIGH > MEDIUM > LOW > None)
        sorter.setComparator(0) { o1, o2 ->
            val p1 = when (o1?.toString()) {
                "HIGH" -> 0
                "MEDIUM" -> 1
                "LOW" -> 2
                else -> 3  // None or null
            }
            val p2 = when (o2?.toString()) {
                "HIGH" -> 0
                "MEDIUM" -> 1
                "LOW" -> 2
                else -> 3  // None or null
            }
            p1.compareTo(p2)
        }
        
        // Column 5: Line - Numeric sorting
        sorter.setComparator(5) { o1, o2 ->
            val n1 = o1?.toString()?.toIntOrNull() ?: Int.MAX_VALUE
            val n2 = o2?.toString()?.toIntOrNull() ?: Int.MAX_VALUE
            n1.compareTo(n2)
        }
        
        // Columns 1, 2 (Assignee, Category): String sorting with nulls last
        for (col in listOf(1, 2)) {
            sorter.setComparator(col) { o1, o2 ->
                val s1 = o1?.toString() ?: ""
                val s2 = o2?.toString() ?: ""
                when {
                    s1.isEmpty() && s2.isEmpty() -> 0
                    s1.isEmpty() -> 1  // Nulls last
                    s2.isEmpty() -> -1
                    else -> s1.compareTo(s2, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Export TODOs to file
     */
    private fun exportTodos(format: String) {
        // Get TODOs to export (filtered or all)
        val todosToExport = if (filteredTodos.isNotEmpty()) filteredTodos else allTodos
        
        if (todosToExport.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Nothing to export", "No TODO items found.", NotificationType.WARNING)
                .notify(project)
            return
        }
        
        // Create file descriptor
        val descriptor = FileSaverDescriptor(
            "Export TODOs",
            "Save TODO list as ${format.uppercase()}"
        )
        
        // Show file chooser
        val fileSaver = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val virtualFileWrapper = fileSaver.save(null as VirtualFile?, "todos.$format") ?: return
        
        try {
            val file = virtualFileWrapper.file
            val content = when (format) {
                "csv" -> TodoExporter().exportToCsv(todosToExport)
                "md" -> TodoExporter().exportToMarkdown(todosToExport)
                else -> ""
            }
            
            file.writeText(content)
            
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Export Successful", "Saved ${todosToExport.size} TODOs to ${file.name}", NotificationType.INFORMATION)
                .notify(project)
                
        } catch (e: Exception) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Export Failed", e.message ?: "Unknown error", NotificationType.ERROR)
                .notify(project)
        }
    }
    
    /**
     * Setup auto-refresh on file save
     */
    private fun setupAutoRefresh() {
        val connection: MessageBusConnection = project.messageBus.connect()
        
        // Debounce timer to prevent rapid refreshes
        val refreshTimer = Timer(500) {
            ApplicationManager.getApplication().invokeLater {
                scanProject()
            }
        }
        refreshTimer.isRepeats = false
        
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                // Check if any relevant files were changed
                val relevantChanges = events.any { event ->
                    val file = event.file
                    file != null && !file.isDirectory && isValidFileType(file.name)
                }
                
                if (relevantChanges) {
                    refreshTimer.restart()
                }
            }
        })
    }
    
    private fun isValidFileType(fileName: String): Boolean {
        val extensions = listOf("kt", "java", "xml", "js", "ts", "py", "go", "rs", "cpp", "c", "h", "cs", "swift", "rb", "php", "scala", "groovy")
        return extensions.any { fileName.endsWith(".$it", ignoreCase = true) }
    }
}
