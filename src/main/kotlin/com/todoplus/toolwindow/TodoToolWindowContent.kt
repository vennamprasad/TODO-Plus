package com.todoplus.toolwindow

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.Gray
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.intellij.ide.BrowserUtil
import com.todoplus.exporter.TodoExporter
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
    private val priorityFilter = JComboBox<String>()
    private val assigneeFilter = JBTextField()
    private val categoryFilter = JBTextField()
    private val searchField = JBTextField()

    init {
        // Initialize table with columns
        tableModel.addColumn("Priority")
        tableModel.addColumn("Assignee")
        tableModel.addColumn("Category")
        tableModel.addColumn("Due Date")
        tableModel.addColumn("Description")
        tableModel.addColumn("Tags")
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
            
            // Specific column widths
            columnModel.getColumn(0).preferredWidth = 80  // Priority
            columnModel.getColumn(1).preferredWidth = 100 // Assignee
            columnModel.getColumn(2).preferredWidth = 100 // Category
            columnModel.getColumn(3).preferredWidth = 100 // Due Date
            columnModel.getColumn(4).preferredWidth = 400 // Description
            columnModel.getColumn(5).preferredWidth = 150 // Tags
            columnModel.getColumn(6).preferredWidth = 150 // File
            columnModel.getColumn(7).preferredWidth = 50  // Line
            
            // Add click listener for navigation and context menu
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        navigateToSelectedTodo()
                    }
                    
                    // Right-click context menu
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val row = rowAtPoint(e.point)
                        if (row >= 0 && row < rowCount) {
                            setRowSelectionInterval(row, row)
                            showContextMenu(e, filteredTodos[convertRowIndexToModel(row)])
                        }
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
        
        // Populate priority filter
        updatePriorityFilter()
    }
    
    private fun updatePriorityFilter() {
        val currentSelection = priorityFilter.selectedItem as? String
        priorityFilter.removeAllItems()
        priorityFilter.addItem("All Priorities")
        

        
        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        settings.getPriorities().forEach { 
            priorityFilter.addItem(it.name) 
        }
        priorityFilter.addItem("None")
        
        if (currentSelection != null) {
            // Restore selection if possible, or default to All
            // We need to check if the selection still exists in the model
             var found = false
             for (i in 0 until priorityFilter.itemCount) {
                 if (priorityFilter.getItemAt(i) == currentSelection) {
                     priorityFilter.selectedItem = currentSelection
                     found = true
                     break
                 }
             }
             if (!found) priorityFilter.selectedIndex = 0
        } else {
             priorityFilter.selectedIndex = 0
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
                if (searchText.contains(":")) {
                   // key:value search
                   val parts = searchText.split(":")
                   if (parts.size >= 2) {
                       val key = parts[0].trim()
                       val value = parts[1].trim()
                       
                        matches = when(key) {
                            "priority" -> todo.priority?.name?.lowercase() == value
                            "assignee", "assigned" -> todo.assignee?.lowercase()?.contains(value) == true
                            "category" -> todo.category?.lowercase()?.contains(value) == true
                            else -> todo.tags[key]?.lowercase()?.contains(value) == true
                        }
                   }
                } else {
                    matches = todo.description.lowercase().contains(searchText)
                }
            }
            
            if (matches) {
                filteredTodos.add(todo)
                tableModel.addRow(arrayOf(
                    todo.priority?.name ?: "-",
                    if (todo.assignee != null) "@${todo.assignee}" else "-",
                    todo.category ?: "-",
                    todo.dueDate ?: "-",  // Store LocalDate object or "-" string
                    todo.description,
                    todo.tags.entries.filter { it.key != "due" }.joinToString(", ") { "${it.key}:${it.value}" }.ifEmpty { "-" },
                    todo.getFileName(),
                    todo.lineNumber
                ))
            }
        }
        
        updateStatistics()
    }
    
    private fun updateStatistics() {
        val count = filteredTodos.size
        // Calculate counts for each configured priority
        val priorityCounts = mutableMapOf<String, Int>()
        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        
        settings.getPriorities().forEach { config ->
            val pCount = filteredTodos.count { it.priority?.name == config.name }
            if (pCount > 0) {
                priorityCounts[config.name] = pCount
            }
        }
        
        val missingPriority = filteredTodos.count { it.priority == null }
        val missingAssignee = filteredTodos.count { it.assignee == null }
        
        val sb = StringBuilder("Found $count TODOs")
        if (count > 0) {
            if (priorityCounts.isNotEmpty()) {
                val stats = priorityCounts.map { "${it.value} ${it.key.lowercase()}" }.joinToString(", ")
                sb.append(" ($stats)")
            }
            
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
    
    private fun showContextMenu(e: java.awt.event.MouseEvent, todo: TodoItem) {
        val popup = JPopupMenu()
        
        // Navigate
        val navigateItem = JMenuItem("Navigate to Code")
        navigateItem.addActionListener { navigateTo(todo) }
        popup.add(navigateItem)
        
        // Open Issue Tracker
        val issueId = todo.issueId
        val settings = com.todoplus.settings.TodoSettingsService.getInstance().getState()
        val template = settings.issueUrlTemplate
        
        if (issueId != null && template.isNotEmpty() && template.contains("{id}")) {
            val openIssueItem = JMenuItem("Open Issue $issueId")
            openIssueItem.addActionListener {
                val url = template.replace("{id}", issueId)
                try {
                    BrowserUtil.browse(url)
                } catch (ex: Exception) {
                    // Ignore browser errors
                }
            }
            popup.add(openIssueItem)
        }
        
        popup.show(e.component, e.x, e.y)
    }

    private fun navigateTo(todo: TodoItem?) {
        if (todo == null) return
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(todo.filePath)
        if (virtualFile != null) {
            // Navigate to file and line
            FileEditorManager.getInstance(project).openTextEditor(
                OpenFileDescriptor(project, virtualFile, todo.lineNumber - 1, 0),
                true
            )
        }
    }
    
    private fun navigateToSelectedTodo() {
        val selectedRow = table.selectedRow
        if (selectedRow != -1) {
            // Convert view row index to model row index in case of sorting
            val modelRow = table.convertRowIndexToModel(selectedRow)
            val todo = filteredTodos[modelRow]
            navigateTo(todo)
        }
    }
    
    /**
     * Custom cell renderer for priority column
     */
    private class PriorityColorRenderer : DefaultTableCellRenderer() {
        
        private val settingsService get() = com.todoplus.settings.TodoSettingsService.getInstance()
        
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
                        val priorityName = value?.toString()
                        if (priorityName != null && priorityName != "-") {
                            val color = settingsService.getPriorityColor(priorityName)
                            
                            if (color != null) {
                                component.foreground = color
                            } else {
                                // Fallback for unknown priorities
                                component.foreground = Gray._100
                            }
                        } else {
                            // No priority - use italic gray
                            component.font = component.font.deriveFont(Font.ITALIC)
                            component.foreground = Gray._150
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
                    3 -> {
                         // Due Date column
                        if (value == null || value.toString() == "-") {
                            component.foreground = Gray._150
                            component.font = component.font.deriveFont(Font.ITALIC)
                        } else {
                             try {
                                 // Check if value is LocalDate (optimization) or String
                                 val date = if (value is java.time.LocalDate) value else java.time.LocalDate.parse(value.toString())
                                 val today = java.time.LocalDate.now()
                                 
                                 if (date.isBefore(today)) {
                                     // Overdue - Red
                                     component.foreground = Color(220, 50, 50)
                                     component.font = component.font.deriveFont(Font.BOLD)
                                 } else if (date.isBefore(today.plusDays(7))) {
                                     // Due soon (within 7 days) - Orange
                                     component.foreground = Color(220, 160, 30)
                                 } else {
                                     component.foreground = table.foreground
                                 }
                             } catch (e: Exception) {
                                 component.foreground = table.foreground
                             }
                        }
                    }
                    4 -> { 
                        // Description column
                        component.foreground = table.foreground
                    }
                    5 -> {
                        // Tags column
                         if (value?.toString() == "-") {
                            component.foreground = Gray._150
                            component.font = component.font.deriveFont(Font.ITALIC)
                        } else {
                            component.foreground = Color(100, 100, 150) // Bluish for tags
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
        
        // Column 0: Priority - Custom comparator based on settings order
        sorter.setComparator(0) { o1, o2 ->
            val p1Name = o1?.toString()
            val p2Name = o2?.toString()
            
            val settings = com.todoplus.settings.TodoSettingsService.getInstance()
            val priorities = settings.getPriorities()
            
            // Find index in settings list
            // -1 if not found
            val idx1 = if (p1Name == null || p1Name == "-") Int.MAX_VALUE else priorities.indexOfFirst { it.name == p1Name }
            val idx2 = if (p2Name == null || p2Name == "-") Int.MAX_VALUE else priorities.indexOfFirst { it.name == p2Name }
            
            val effectiveIdx1 = if (idx1 == -1) Int.MAX_VALUE - 1 else idx1 // Unknown priorities go to bottom but above null
            val effectiveIdx2 = if (idx2 == -1) Int.MAX_VALUE - 1 else idx2
            
            effectiveIdx1.compareTo(effectiveIdx2)
        }
        
        // Column 7: Line - Numeric sorting
        sorter.setComparator(7) { o1, o2 ->
            val n1 = o1?.toString()?.toIntOrNull() ?: Int.MAX_VALUE
            val n2 = o2?.toString()?.toIntOrNull() ?: Int.MAX_VALUE
            n1.compareTo(n2)
        }
        
        // Columns 1, 2 (Assignee, Category), 3 (Due Date): String/Date sorting with nulls last
        for (col in listOf(1, 2, 3)) {
            sorter.setComparator(col) { o1, o2 ->
                val s1 = o1?.toString() ?: ""
                val s2 = o2?.toString() ?: ""
                when {
                    s1 == "-" && s2 == "-" -> 0
                    s1 == "-" -> 1  // Nulls last
                    s2 == "-" -> -1
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
        val todosToExport = filteredTodos.ifEmpty { allTodos }
        
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
