package com.todoplus.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColorPicker
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.GridLayout
import javax.swing.*

/**
 * Settings page for TODO++ configuration
 */
class TodoSettingsConfigurable : Configurable {

    private var settingsPanel: JPanel? = null
    private val priorityListModel = DefaultListModel<TodoSettingsService.PriorityConfig>()
    private lateinit var priorityList: JBList<TodoSettingsService.PriorityConfig>
    private val issueUrlField = JTextField()
    private val issuePatternField = JTextField()
    private var isModified = false

    override fun getDisplayName(): String = "TODO++"

    override fun createComponent(): JComponent? {
        settingsPanel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        
        // Priority Settings
        val currentSettings = TodoSettingsService.getInstance().getPriorities()
        priorityListModel.clear()
        currentSettings.forEach { priorityListModel.addElement(it.copy()) } // Deep copy
        
        priorityList = JBList(priorityListModel).apply {
            cellRenderer = PriorityListRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }
        
        val decorator = ToolbarDecorator.createDecorator(priorityList)
            .setAddAction { addPriority() }
            .setRemoveAction { removePriority() }
            .setMoveUpAction { movePriority(-1) }
            .setMoveDownAction { movePriority(1) }
            .setEditAction { editPriority() }
            
        val listPanel = decorator.createPanel()
        listPanel.border = BorderFactory.createTitledBorder("Priority Levels (Ordered High to Low)")
        mainPanel.add(listPanel)
        
        // Issue Tracker Settings
        val issuePanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Issue Tracker Integration")
            
            val formPanel = JPanel(GridLayout(2, 2, 5, 5))
            formPanel.add(JLabel("Issue URL Template:"))
            formPanel.add(issueUrlField)
            formPanel.add(JLabel("Issue ID Pattern (Regex):"))
            formPanel.add(issuePatternField)
            
            val hintLabel = JLabel("<html><small>Use <b>{id}</b> placeholder in URL. Example: https://github.com/user/repo/issues/<b>{id}</b></small></html>")
            hintLabel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            
            add(formPanel, BorderLayout.CENTER)
            add(hintLabel, BorderLayout.SOUTH)
        }
        mainPanel.add(issuePanel)
        
        // Load Issue Settings
        val settings = TodoSettingsService.getInstance()
        issueUrlField.text = settings.getState().issueUrlTemplate
        issuePatternField.text = settings.getState().issuePattern

        settingsPanel?.add(mainPanel, BorderLayout.CENTER)
        
        return settingsPanel
    }

    private fun addPriority() {
        val panel = settingsPanel ?: return
        val name = Messages.showInputDialog(
            panel,
            "Enter priority name:",
            "Add Priority",
            Messages.getQuestionIcon(),
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    return !inputString.isNullOrBlank() && 
                           !priorityListModel.elements().toList().any { it.name.equals(inputString, ignoreCase = true) }
                }
                override fun canClose(inputString: String?): Boolean = checkInput(inputString)
            }
        )
        
        if (name != null) {
            val color = ColorPicker.showDialog(panel, "Choose Color", Color.GRAY, true, null, false)
            if (color != null) {
                priorityListModel.addElement(TodoSettingsService.PriorityConfig(name.uppercase(), color.rgb))
                isModified = true
            }
        }
    }
    
    private fun removePriority() {
        val index = priorityList.selectedIndex
        if (index != -1) {
            priorityListModel.remove(index)
            isModified = true
        }
    }
    
    private fun movePriority(direction: Int) {
        val index = priorityList.selectedIndex
        if (index != -1) {
            val newIndex = index + direction
            if (newIndex >= 0 && newIndex < priorityListModel.size()) {
                val item = priorityListModel.remove(index)
                priorityListModel.add(newIndex, item)
                priorityList.selectedIndex = newIndex
                isModified = true
            }
        }
    }
    
    private fun editPriority() {
        val panel = settingsPanel ?: return
        val index = priorityList.selectedIndex
        if (index != -1) {
            val current = priorityListModel.get(index)
            val color = ColorPicker.showDialog(panel, "Choose Color for ${current.name}", current.getColor(), true, null, false)
            if (color != null) {
                current.colorRgb = color.rgb
                priorityList.repaint() // Force refresh
                isModified = true
            }
        }
    }

    override fun isModified(): Boolean {
        if (isModified) return true
        
        val settings = TodoSettingsService.getInstance()
        val stored = settings.getPriorities()
        if (stored.size != priorityListModel.size()) return true

        // Check issue settings
        if (issueUrlField.text != settings.getState().issueUrlTemplate) return true
        if (issuePatternField.text != settings.getState().issuePattern) return true
        
        for (i in 0 until stored.size) {
            if (stored[i] != priorityListModel.get(i)) return true
        }
        
        return false
    }

    override fun apply() {
        val newPriorities = priorityListModel.elements().toList()
        val settings = TodoSettingsService.getInstance()
        settings.setPriorities(newPriorities)
        settings.getState().issueUrlTemplate = issueUrlField.text.trim()
        settings.getState().issuePattern = issuePatternField.text.trim()
        isModified = false
    }

    override fun reset() {
        val settings = TodoSettingsService.getInstance()
        val currentSettings = settings.getPriorities()
        priorityListModel.clear()
        currentSettings.forEach { priorityListModel.addElement(it.copy()) }
        issueUrlField.text = settings.getState().issueUrlTemplate
        issuePatternField.text = settings.getState().issuePattern
        isModified = false
    }
    
    // Custom renderer for the list
    private class PriorityListRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is TodoSettingsService.PriorityConfig) {
                text = value.name
                icon = ColorIcon(value.getColor())
            }
            return component
        }
    }
    
    // Helper for color icon
    private class ColorIcon(private val color: Color) : Icon {
        override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
            g.color = color
            g.fillRect(x, y, iconWidth, iconHeight)
            g.color = Color.GRAY
            g.drawRect(x, y, iconWidth, iconHeight)
        }
        override fun getIconWidth(): Int = 16
        override fun getIconHeight(): Int = 16
    }
}
