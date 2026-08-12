package com.todoplus.toolwindow

import com.intellij.ide.BrowserUtil
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
import com.intellij.ui.dualView.TreeTableView
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import com.todoplus.exporter.TodoExporter
import com.todoplus.models.TodoItem
import com.todoplus.services.TodoScannerService
import com.todoplus.ui.tree.TodoGroupBy
import com.todoplus.ui.tree.TodoTreeModelBuilder
import com.todoplus.ui.tree.TodoTreeTableColumns
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

import javax.swing.tree.DefaultMutableTreeNode

import com.intellij.openapi.Disposable
import com.intellij.util.Alarm

/**
 * Content panel for the TODO++ tool window
 */
class TodoToolWindowContent(private val project: Project) : Disposable {

    private val filterAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    override fun dispose() {
        filterAlarm.cancelAllRequests()
    }

        private val treeTable: TreeTableView
    private val groupByDropdown = JComboBox(TodoGroupBy.entries.toTypedArray())
    private val mainPanel: SimpleToolWindowPanel
    private val statusLabel: JBLabel
    private val allTodos = mutableListOf<TodoItem>()
    private val filteredTodos = mutableListOf<TodoItem>()
    private var showCompleted: Boolean = true
    
    // Filter controls
    private val priorityFilter = JComboBox<String>()
    private val assigneeFilter = JBTextField()
    private val categoryFilter = JBTextField()
    private val searchField = SearchTextField()
    
    // Scope control
    private val scopeDropdown = JComboBox(arrayOf("Entire Solution / Project", "Current File"))
    
    // Progress Bar
    private val progressBar = object : JProgressBar(0, 100) {
        override fun paintComponent(g: java.awt.Graphics) {
            super.paintComponent(g)
            val str = string
            if (!str.isNullOrEmpty()) {
                val g2 = g.create() as java.awt.Graphics2D
                try {
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    g2.font = font
                    val fm = g2.fontMetrics
                    val textWidth = fm.stringWidth(str)
                    val textHeight = fm.ascent
                    val x = (width - textWidth) / 2
                    val y = (height + textHeight) / 2 - 1
                    
                    // Draw subtle text shadow
                    g2.color = com.intellij.ui.JBColor(java.awt.Color(0, 0, 0, 80), java.awt.Color(0, 0, 0, 120))
                    g2.drawString(str, x + 1, y + 1)
                    
                    // Draw crisp white text
                    g2.color = com.intellij.ui.JBColor(java.awt.Color(30, 30, 30), java.awt.Color(255, 255, 255))
                    g2.drawString(str, x, y)
                } finally {
                    g2.dispose()
                }
            }
        }
    }.apply {
        isStringPainted = false // Handled in custom paintComponent for 100% white text
        preferredSize = java.awt.Dimension(140, 18)
        toolTipText = "Task Completion Progress"
        foreground = com.intellij.ui.JBColor(java.awt.Color(60, 160, 80), java.awt.Color(50, 140, 70))
        background = com.intellij.ui.JBColor(java.awt.Color(230, 230, 230), java.awt.Color(50, 52, 54))
    }

        init {
        treeTable = TreeTableView(TodoTreeModelBuilder.buildModel(emptyList(), TodoGroupBy.NONE, TodoTreeTableColumns.createColumns())).apply {
            setDefaultEditor(Any::class.java, null)
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            
            // Custom renderers are now handled by TodoColumnInfo descriptors
            
            // Add click listener for navigation and context menu
            val popupListener = object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        navigateToSelectedTodo()
                    }
                    checkPopup(e)
                }

                override fun mousePressed(e: MouseEvent) {
                    checkPopup(e)
                }

                override fun mouseReleased(e: MouseEvent) {
                    checkPopup(e)
                }

                private fun checkPopup(e: MouseEvent) {
                    if (e.isPopupTrigger || SwingUtilities.isRightMouseButton(e)) {
                        val row = rowAtPoint(e.point)
                        if (row >= 0 && row < rowCount) {
                            if (!isRowSelected(row)) {
                                setRowSelectionInterval(row, row)
                            }
                            val selectedTodos = getSelectedTodos()
                            if (selectedTodos.isNotEmpty()) {
                                showContextMenu(e, selectedTodos)
                            }
                        }
                    }
                }
            }
            addMouseListener(popupListener)
            
            // Premium Tree Settings
            tree.isRootVisible = false
            tree.showsRootHandles = true
        }

        // Configure empty text
        treeTable.emptyText.text = "No TODOs found. Click 'Scan Project' to begin."
        
        // Create filter panel
        val filterPanel = createFilterPanel()

        // Create Action Toolbar (Premium UI)
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Refresh", "Refresh TODO list from existing scan", AllIcons.Actions.Refresh) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { performScan() }
            })
            add(object : AnAction("Scan", "Scan selected scope for TODOs", AllIcons.Actions.Find) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { performScan() }
            })
            addSeparator()
            add(object : AnAction("Mark Completed", "Mark selected TODOs as completed (Tick)", AllIcons.Actions.Commit) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) {
                    val todos = getSelectedTodos()
                    if (todos.isNotEmpty()) {
                        com.todoplus.actions.ToggleTodoCompletedAction.setCompletionForMultiple(project, todos, true) { performScan() }
                    }
                }
            })
            add(object : AnAction("Mark Incomplete", "Mark selected TODOs as incomplete (Cross)", AllIcons.Actions.Cancel) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) {
                    val todos = getSelectedTodos()
                    if (todos.isNotEmpty()) {
                        com.todoplus.actions.ToggleTodoCompletedAction.setCompletionForMultiple(project, todos, false) { performScan() }
                    }
                }
            })
            add(object : ToggleAction("Show Completed Tasks", "Show or hide completed TODO tasks", AllIcons.Actions.Show) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun isSelected(e: AnActionEvent): Boolean = showCompleted
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    showCompleted = state
                    applyFilters()
                    updateStatistics()
                }
            })
            addSeparator()
            add(object : AnAction("Export CSV", "Export TODOs to CSV file", AllIcons.ToolbarDecorator.Export) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { exportTodos("csv") }
            })
            add(object : AnAction("Export Markdown", "Export TODOs to Markdown file", AllIcons.FileTypes.Text) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { exportTodos("md") }
            })
            add(object : AnAction("Export HTML Dashboard", "Generate and open HTML Dashboard", AllIcons.FileTypes.Html) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { exportTodos("html") }
            })
            add(object : AnAction("Export PDF Report", "Generate printable PDF report", AllIcons.Actions.MenuOpen) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { exportTodos("pdf") }
            })
            add(object : AnAction("Copy for Standup", "Copy formatted TODO list to Clipboard for Slack/Teams standup", AllIcons.Actions.Copy) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { copyForStandup() }
            })
            addSeparator()
            add(object : AnAction("Export Task to GitHub Issue", "Create a new issue on GitHub for selected TODO", AllIcons.Vcs.Vendors.Github) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { exportSelectedToGitHubIssue() }
            })
            add(object : AnAction("Export Task to Jira Issue", "Create a new issue on Jira for selected TODO", AllIcons.Actions.Commit) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { exportSelectedToJiraIssue() }
            })
            add(object : AnAction("Send Overdue Webhook Alerts", "Send Slack / Discord alerts for overdue tasks", AllIcons.General.Warning) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { sendOverdueWebhookAlerts() }
            })
            addSeparator()
            add(object : AnAction("Clear Filters", "Clear all search filters", AllIcons.Actions.GC) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { clearFilters() }
            })
            addSeparator()
            add(object : AnAction("Expand All", "Expand all groups", AllIcons.Actions.Expandall) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { TreeUtil.expandAll(treeTable.tree) }
            })
            add(object : AnAction("Collapse All", "Collapse all groups", AllIcons.Actions.Collapseall) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) { TreeUtil.collapseAll(treeTable.tree, 0) }
            })
        }
        
        val actionToolbar = ActionManager.getInstance().createActionToolbar("TodoToolWindowToolbar", actionGroup, true)
        actionToolbar.targetComponent = treeTable

        statusLabel = JBLabel(" Ready. ")
        statusLabel.border = JBUI.Borders.empty(2, 5)
        statusLabel.foreground = Gray._120

        // Create main panel using SimpleToolWindowPanel
        mainPanel = SimpleToolWindowPanel(true).apply {
            val topPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.customLineBottom(Gray._200)
                add(actionToolbar.component, BorderLayout.WEST)
                add(filterPanel, BorderLayout.CENTER)
            }
            
            toolbar = topPanel
            setContent(JBScrollPane(treeTable as Component))
            
            val bottomPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.customLineTop(Gray._200)
                add(statusLabel, BorderLayout.WEST)
                add(progressBar, BorderLayout.EAST)
            }
            add(bottomPanel, BorderLayout.SOUTH)
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
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 2)).apply {
            border = JBUI.Borders.empty(2, 5)
        }

        // Group By dropdown
        panel.add(JLabel("Group By:"))
        groupByDropdown.addActionListener { applyFilters() }
        panel.add(groupByDropdown)

        // Scope dropdown
        panel.add(JLabel("Scope:"))
        scopeDropdown.addActionListener { performScan() }
        panel.add(scopeDropdown)

        // Priority filter
        panel.add(JLabel("Priority:"))
        priorityFilter.addActionListener { applyFilters() }
        panel.add(priorityFilter)

        // Assignee filter
        panel.add(JLabel("Person:"))
        assigneeFilter.columns = 8
        assigneeFilter.toolTipText = "Author/Assignee..."
        panel.add(assigneeFilter)

        // Category filter
        panel.add(JLabel("Category:"))
        categoryFilter.columns = 8
        categoryFilter.toolTipText = "Bug/Feature..."
        panel.add(categoryFilter)
        
        // Search text field (Premium UI)
        searchField.toolTipText = "Search description..."
        panel.add(searchField)
        
        // Apply filter with 200ms debounce to keep UI fluid on large datasets
        val debouncedApply = {
            filterAlarm.cancelAllRequests()
            filterAlarm.addRequest({ applyFilters() }, 200)
        }

        val docListener = object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = debouncedApply()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = debouncedApply()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = debouncedApply()
        }

        assigneeFilter.document.addDocumentListener(docListener)
        categoryFilter.document.addDocumentListener(docListener)
        searchField.textEditor.document.addDocumentListener(docListener)
        
        val applyAction = java.awt.event.ActionListener { applyFilters() }
        assigneeFilter.addActionListener(applyAction)
        categoryFilter.addActionListener(applyAction)
        searchField.textEditor.addActionListener(applyAction)

        val filterButton = JButton("Apply").apply {
            addActionListener(applyAction)
            isOpaque = false
        }
        panel.add(filterButton)

        return panel
    }

    fun getContent(): JComponent = mainPanel

    private fun performScan() {
        if (scopeDropdown.selectedItem == "Current File") {
            scanCurrentFile()
        } else {
            scanProject()
        }
    }

    private fun scanCurrentFile() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val document = editor?.document
        val file = document?.let { com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(it) }
        
        if (file == null) {
            statusLabel.text = "No active file selected."
            return
        }
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning Current File", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Scanning ${file.name}..."
                indicator.isIndeterminate = true
                
                try {
                    val scanner = project.service<TodoScannerService>()
                    val foundTodos = scanner.scanFile(file)
                    
                    ApplicationManager.getApplication().invokeLater {
                        allTodos.clear()
                        allTodos.addAll(foundTodos)
                        
                        applyFilters()
                        updateStatistics()
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "Error scanning file: ${e.message}"
                    }
                }
            }
        })
    }

    private fun scanProject() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning for TODOs", true) {
            override fun run(indicator: ProgressIndicator) {
                // Background thread
                indicator.text = "Scanning project files..."
                indicator.isIndeterminate = true
                
                try {
                    val scanner = project.service<TodoScannerService>()
                    // Run scanning (service handles Read Actions internally and updates progress indicator)
                    val foundTodos = scanner.scanProject(indicator)
                    
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
        val priorityFilterValue = priorityFilter.selectedItem as String
        val assigneeText = assigneeFilter.text.trim().removePrefix("@").lowercase()
        val categoryText = categoryFilter.text.trim().lowercase()
        val searchText = searchField.text.trim().lowercase()
        
        // Parse structured tags in search text (e.g. risk:high)
        val tagFilters = mutableMapOf<String, String>()
        val searchTokens = searchText.split(" ").filter { it.isNotBlank() }
        val remainingSearchTokens = mutableListOf<String>()

        searchTokens.forEach { token ->
            if (token.contains(":") && !token.startsWith(":")) {
                val parts = token.split(":", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    tagFilters[parts[0].lowercase()] = parts[1].lowercase()
                } else {
                    remainingSearchTokens.add(token)
                }
            } else {
                remainingSearchTokens.add(token)
            }
        }
        val cleanSearchText = remainingSearchTokens.joinToString(" ")

        for (todo in allTodos) {
            if (!showCompleted && todo.isCompleted) continue
            
            var matches = true
            
            // Priority Filter
            if (priorityFilterValue != "All Priorities") {
                if (todo.priority?.name?.uppercase() != priorityFilterValue.uppercase()) {
                    matches = false
                }
            }
            
            // Assignee Filter
            if (matches && assigneeText.isNotEmpty()) {
                val hasAssignee = todo.assignee?.lowercase()?.contains(assigneeText) == true
                val hasAuthor = todo.vcsAuthor?.lowercase()?.contains(assigneeText) == true
                if (!hasAssignee && !hasAuthor) {
                    matches = false
                }
            }
            
            // Category Filter
            if (matches && categoryText.isNotEmpty()) {
                if (todo.category?.lowercase()?.contains(categoryText) != true) {
                    matches = false
                }
            }
            
            // Structured Tag Filters
            if (matches && tagFilters.isNotEmpty()) {
                for ((key, value) in tagFilters) {
                    val tagMatch = when(key) {
                        "priority" -> todo.priority?.name?.lowercase() == value
                        "assignee", "assigned", "author" -> {
                            val hasAssignee = todo.assignee?.lowercase()?.contains(value) == true
                            val hasAuthor = todo.vcsAuthor?.lowercase()?.contains(value) == true
                            hasAssignee || hasAuthor
                        }
                        "category" -> todo.category?.lowercase()?.contains(value) == true
                        else -> todo.tags[key]?.lowercase()?.contains(value) == true
                    }
                    if (!tagMatch) {
                        matches = false
                        break
                    }
                }
            }
            
            // General Text Search
            if (matches && cleanSearchText.isNotEmpty()) {
                val inDesc = todo.description.lowercase().contains(cleanSearchText)
                val inAssignee = todo.assignee?.lowercase()?.contains(cleanSearchText) == true
                val inAuthor = todo.vcsAuthor?.lowercase()?.contains(cleanSearchText) == true
                val inCategory = todo.category?.lowercase()?.contains(cleanSearchText) == true
                val inFileName = todo.getFileName().lowercase().contains(cleanSearchText)
                val inFilePath = todo.filePath.lowercase().contains(cleanSearchText)
                val inIssue = todo.issueId?.lowercase()?.contains(cleanSearchText) == true
                val inTags = todo.tags.entries.any { "${it.key}:${it.value}".lowercase().contains(cleanSearchText) }
                
                if (!inDesc && !inAssignee && !inAuthor && !inCategory && !inFileName && !inFilePath && !inIssue && !inTags) {
                    matches = false
                }
            }
            
            if (matches) {
                filteredTodos.add(todo)
            }
        }
        
        val newModel = TodoTreeModelBuilder.buildModel(filteredTodos, groupByDropdown.selectedItem as TodoGroupBy, TodoTreeTableColumns.createColumns())
        treeTable.setModel(newModel)

        // Smart tree expansion: for large datasets (> 200 items), expand top-level group nodes only to keep rendering UI fast
        if (filteredTodos.size > 200) {
            val rootNode = treeTable.tree.model.root as? DefaultMutableTreeNode
            if (rootNode != null) {
                val childCount = rootNode.childCount
                for (i in 0 until childCount) {
                    val child = rootNode.getChildAt(i)
                    val path = javax.swing.tree.TreePath(arrayOf(rootNode, child))
                    treeTable.tree.expandPath(path)
                }
            }
        } else {
            TreeUtil.expandAll(treeTable.tree)
        }
        
        // Adjust widths after setting model
        val cols = treeTable.columnModel
        if (cols.columnCount >= 11) {
            cols.getColumn(0).preferredWidth = 350 // Item/Group
            cols.getColumn(1).preferredWidth = 80  // Priority
            cols.getColumn(2).preferredWidth = 100 // Author
            cols.getColumn(3).preferredWidth = 100 // Assignee
            cols.getColumn(4).preferredWidth = 100 // Category
            cols.getColumn(5).preferredWidth = 100 // Issue
            cols.getColumn(6).preferredWidth = 100 // Date
            cols.getColumn(7).preferredWidth = 100 // Due Date
            cols.getColumn(8).preferredWidth = 150 // Tags
            cols.getColumn(9).preferredWidth = 200 // File
            cols.getColumn(10).preferredWidth = 50  // Line
        }
        
        updateStatistics()
    }
    
    private fun updateStatistics() {
        val totalCount = allTodos.size
        val completedCount = allTodos.count { it.isCompleted }
        val pendingCount = totalCount - completedCount
        val percent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        progressBar.value = percent
        progressBar.string = "$percent% ($completedCount/$totalCount)"

        val count = filteredTodos.size
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
        
        val sb = StringBuilder("Found $count TODOs ($completedCount DONE, $pendingCount pending)")
        if (count > 0) {
            if (priorityCounts.isNotEmpty()) {
                val stats = priorityCounts.map { "${it.value} ${it.key.lowercase()}" }.joinToString(", ")
                sb.append(" [$stats]")
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
        groupByDropdown.selectedIndex = 0
        priorityFilter.selectedIndex = 0
        assigneeFilter.text = ""
        categoryFilter.text = ""
        searchField.text = ""
        applyFilters()
    }
    
    private fun refreshTodos() {
        performScan()
    }
    
    private fun getSelectedTodos(): List<TodoItem> {
        val selectedRows = treeTable.selectedRows
        val todos = mutableListOf<TodoItem>()
        for (row in selectedRows) {
            val node = treeTable.tree.getPathForRow(row)?.lastPathComponent as? DefaultMutableTreeNode
            val userObj = node?.userObject
            if (userObj is TodoItem) {
                todos.add(userObj)
            } else if (node != null && node.childCount > 0) {
                val childTodos = mutableListOf<TodoItem>()
                collectLeafTodos(node, childTodos)
                todos.addAll(childTodos)
            }
        }
        return todos.distinct()
    }

    private fun collectLeafTodos(node: DefaultMutableTreeNode, result: MutableList<TodoItem>) {
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
            val obj = child.userObject
            if (obj is TodoItem) {
                result.add(obj)
            } else {
                collectLeafTodos(child, result)
            }
        }
    }
    
    private fun showContextMenu(e: java.awt.event.MouseEvent, todos: List<TodoItem>) {
        if (todos.isEmpty()) return
        val popup = JPopupMenu()
        
        val markCompletedText = if (todos.size > 1) "Mark ${todos.size} Selected as Completed" else "Mark as Completed"
        val markCompletedItem = JMenuItem(markCompletedText)
        markCompletedItem.addActionListener {
            com.todoplus.actions.ToggleTodoCompletedAction.setCompletionForMultiple(project, todos, true) { performScan() }
        }
        popup.add(markCompletedItem)

        val markIncompleteText = if (todos.size > 1) "Mark ${todos.size} Selected as Incomplete" else "Mark as Incomplete"
        val markIncompleteItem = JMenuItem(markIncompleteText)
        markIncompleteItem.addActionListener {
            com.todoplus.actions.ToggleTodoCompletedAction.setCompletionForMultiple(project, todos, false) { performScan() }
        }
        popup.add(markIncompleteItem)

        val copyStandupItem = JMenuItem("Copy for Standup (Slack/Markdown)")
        copyStandupItem.addActionListener { copyForStandup() }
        popup.add(copyStandupItem)

        popup.addSeparator()

        if (todos.size == 1) {
            val todo = todos.first()
            val navigateItem = JMenuItem("Navigate to Code")
            navigateItem.addActionListener { navigateTo(todo) }
            popup.add(navigateItem)
            
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
        val selectedRow = treeTable.selectedRow
        if (selectedRow != -1) {
            val node = treeTable.tree.getPathForRow(selectedRow)?.lastPathComponent as? DefaultMutableTreeNode
            val todo = node?.userObject as? TodoItem
            if (todo != null) navigateTo(todo)
        }
    }
    

    
    /**
     * Copy TODOs formatted for Daily Standup (Slack / Teams / Markdown) to Clipboard
     */
    private fun copyForStandup() {
        val selected = getSelectedTodos()
        val todosToCopy = selected.ifEmpty { filteredTodos.ifEmpty { allTodos } }

        if (todosToCopy.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Nothing to copy", "No TODO items found.", NotificationType.WARNING)
                .notify(project)
            return
        }

        val text = buildString {
            appendLine("*Daily Standup Update (${java.time.LocalDate.now()}):*")
            todosToCopy.forEach { todo ->
                val check = if (todo.isCompleted) "[x]" else "[ ]"
                val meta = mutableListOf<String>()
                if (todo.priority != null) meta.add("priority:${todo.priority.name.lowercase()}")
                if (todo.assignee != null) meta.add("@${todo.assignee}")
                if (todo.dueDate != null) meta.add("due:${todo.dueDate}")
                if (todo.issueId != null) meta.add("issue:${todo.issueId}")
                val metaStr = if (meta.isNotEmpty()) " (${meta.joinToString(" ")})" else ""
                appendLine("- $check ${todo.description}$metaStr")
            }
        }

        com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(java.awt.datatransfer.StringSelection(text))

        NotificationGroupManager.getInstance()
            .getNotificationGroup("TODO++ Notifications")
            .createNotification("Copied for Standup", "Copied ${todosToCopy.size} task(s) to Clipboard in Markdown/Slack format.", NotificationType.INFORMATION)
            .notify(project)
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
            "Save TODO list as ${format.uppercase()}",
            format
        )
        
        // Show file chooser
        val fileSaver = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val virtualFileWrapper = fileSaver.save(null as VirtualFile?, "todos.$format") ?: return
        
        try {
            val file = virtualFileWrapper.file
            val content = when (format) {
                "csv"  -> TodoExporter().exportToCsv(todosToExport)
                "md"   -> TodoExporter().exportToMarkdown(todosToExport)
                "html" -> TodoExporter().exportToHtml(
                    todosToExport,
                    com.todoplus.settings.TodoSettingsService.getInstance().getHtmlExportConfig()
                )
                "pdf"  -> TodoExporter().exportToPdf(todosToExport)
                else   -> ""
            }
            
            file.writeText(content)
            
            // Auto-open HTML Dashboard / PDF Report
            if (format == "html" || format == "pdf") {
                BrowserUtil.browse(file.toURI().toString())
            }
            
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
    /**
     * Setup auto-refresh on file save and live editor typing
     */
    private fun setupAutoRefresh() {
        val connection: MessageBusConnection = project.messageBus.connect(this)
        
        // Debounce timer (500ms) to prevent rapid refreshes while typing
        val refreshTimer = Timer(500) {
            ApplicationManager.getApplication().invokeLater {
                performScan()
            }
        }
        refreshTimer.isRepeats = false
        
        // 1. Listen to Virtual File System changes (on disk save/external edits)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val relevantChanges = events.any { event ->
                    val file = event.file
                    file != null && isValidFileType(file)
                }
                
                if (relevantChanges) {
                    refreshTimer.restart()
                }
            }
        })

        // 2. Listen to live editor typing/editing in open documents
        val docListener = object : com.intellij.openapi.editor.event.DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                val file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(event.document)
                if (file != null && isValidFileType(file)) {
                    refreshTimer.restart()
                }
            }
        }
        com.intellij.openapi.editor.EditorFactory.getInstance().eventMulticaster.addDocumentListener(docListener, connection)
    }
    
    private fun isValidFileType(file: VirtualFile): Boolean {
        if (file.isDirectory || !file.isValid) return false
        val ft = file.fileType
        return !ft.isBinary && ft.name.uppercase() != "UNKNOWN"
    }

    private fun getSelectedTodo(): TodoItem? {
        val selectedRow = treeTable.selectedRow
        if (selectedRow < 0) return null
        val node = treeTable.getValueAt(selectedRow, 0)
        if (node is DefaultMutableTreeNode && node.userObject is TodoItem) {
            return node.userObject as TodoItem
        }
        return null
    }

    private fun exportSelectedToGitHubIssue() {
        val todo = getSelectedTodo()
        if (todo == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Please select a TODO item to export to GitHub.", NotificationType.WARNING)
                .notify(project)
            return
        }

        val settings = com.todoplus.settings.TodoSettingsService.getInstance().getState()
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Exporting to GitHub Issue", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val result = com.todoplus.services.integration.IssueExporterService.createGitHubIssue(
                    todo, settings.githubToken, settings.githubRepoOwner, settings.githubRepoName
                )
                ApplicationManager.getApplication().invokeLater {
                    result.onSuccess { url ->
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("TODO++ Notifications")
                            .createNotification("Exported to GitHub Issue: <a href='$url'>$url</a>", NotificationType.INFORMATION)
                            .setListener(com.intellij.notification.NotificationListener.URL_OPENING_LISTENER)
                            .notify(project)
                    }.onFailure { ex ->
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("TODO++ Notifications")
                            .createNotification("GitHub Issue Export Failed: ${ex.message}", NotificationType.ERROR)
                            .notify(project)
                    }
                }
            }
        })
    }

    private fun exportSelectedToJiraIssue() {
        val todo = getSelectedTodo()
        if (todo == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Please select a TODO item to export to Jira.", NotificationType.WARNING)
                .notify(project)
            return
        }

        val settings = com.todoplus.settings.TodoSettingsService.getInstance().getState()
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Exporting to Jira Issue", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val result = com.todoplus.services.integration.IssueExporterService.createJiraIssue(
                    todo, settings.jiraBaseUrl, settings.jiraEmail, settings.jiraApiToken, settings.jiraProjectKey
                )
                ApplicationManager.getApplication().invokeLater {
                    result.onSuccess { url ->
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("TODO++ Notifications")
                            .createNotification("Exported to Jira Issue: <a href='$url'>$url</a>", NotificationType.INFORMATION)
                            .setListener(com.intellij.notification.NotificationListener.URL_OPENING_LISTENER)
                            .notify(project)
                    }.onFailure { ex ->
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("TODO++ Notifications")
                            .createNotification("Jira Issue Export Failed: ${ex.message}", NotificationType.ERROR)
                            .notify(project)
                    }
                }
            }
        })
    }

    private fun sendOverdueWebhookAlerts() {
        val today = java.time.LocalDate.now()
        val overdueTodos = allTodos.filter { !it.isCompleted && it.dueDate != null && it.dueDate.isBefore(today) }

        if (overdueTodos.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("No overdue TODO items found to notify.", NotificationType.INFORMATION)
                .notify(project)
            return
        }

        val settings = com.todoplus.settings.TodoSettingsService.getInstance().getState()
        if (settings.slackWebhookUrl.isBlank() && settings.discordWebhookUrl.isBlank()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("TODO++ Notifications")
                .createNotification("Please configure a Slack or Discord Webhook URL in Settings.", NotificationType.WARNING)
                .notify(project)
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Sending Overdue Webhook Alerts", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                var slackSuccess = false
                var discordSuccess = false

                if (settings.slackWebhookUrl.isNotBlank()) {
                    com.todoplus.services.integration.WebhookNotificationService.sendOverdueSlackNotification(settings.slackWebhookUrl, overdueTodos)
                        .onSuccess { slackSuccess = true }
                }
                if (settings.discordWebhookUrl.isNotBlank()) {
                    com.todoplus.services.integration.WebhookNotificationService.sendOverdueDiscordNotification(settings.discordWebhookUrl, overdueTodos)
                        .onSuccess { discordSuccess = true }
                }

                ApplicationManager.getApplication().invokeLater {
                    val msg = "Sent overdue notifications for ${overdueTodos.size} tasks (Slack: ${if (slackSuccess) "Sent" else "N/A"}, Discord: ${if (discordSuccess) "Sent" else "N/A"})."
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("TODO++ Notifications")
                        .createNotification(msg, NotificationType.INFORMATION)
                        .notify(project)
                }
            }
        })
    }
}
