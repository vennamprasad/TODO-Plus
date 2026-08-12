package com.todoplus.ui.tree

import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.Gray
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.ColumnInfo
import com.todoplus.models.TodoItem
import java.awt.Color
import java.text.SimpleDateFormat
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode

object TodoTreeTableColumns {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    abstract class TodoColumnInfo(name: String) : ColumnInfo<DefaultMutableTreeNode, String>(name) {
        override fun valueOf(node: DefaultMutableTreeNode): String? {
            val userObject = node.userObject
            if (userObject is TodoItem) {
                return getTodoValue(userObject)
            }
            return if (this is ItemColumn) userObject.toString() else null
        }

        abstract fun getTodoValue(todo: TodoItem): String?
    }

    // Default renderer that fades missing "-" values
    abstract class FadeMissingColumnInfo(name: String) : TodoColumnInfo(name) {
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = FadeMissingRenderer
    }

    class ItemColumn : TodoColumnInfo("Item") {
        override fun getTodoValue(todo: TodoItem): String {
            val title = todo.description.substringBefore('\n')
            return if (todo.isCompleted) "✔ $title" else title
        }
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = ItemRenderer
    }

    object ItemRenderer : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
            val str = value?.toString() ?: ""
            if (str.startsWith("✔ ")) {
                val style = SimpleTextAttributes.STYLE_STRIKEOUT or SimpleTextAttributes.STYLE_ITALIC
                append(str, SimpleTextAttributes(style, if (selected) null else Gray._150))
            } else {
                append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }

    class PriorityColumn : TodoColumnInfo("Priority") {
        override fun getTodoValue(todo: TodoItem): String = todo.priority?.name ?: "-"
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = PriorityRenderer
    }

    class AuthorColumn : FadeMissingColumnInfo("Author") {
        override fun getTodoValue(todo: TodoItem): String = todo.vcsAuthor ?: "-"
    }

    class AssigneeColumn : FadeMissingColumnInfo("Assignee") {
        override fun getTodoValue(todo: TodoItem): String = if (todo.assignee != null) "@${todo.assignee}" else "-"
    }

    class CategoryColumn : FadeMissingColumnInfo("Category") {
        override fun getTodoValue(todo: TodoItem): String = todo.category ?: "-"
    }

    class IssueColumn : FadeMissingColumnInfo("Issue") {
        override fun getTodoValue(todo: TodoItem): String = todo.issueId ?: "-"
    }

    class DateAddedColumn : TodoColumnInfo("Date Added") {
        override fun getTodoValue(todo: TodoItem): String = if (todo.vcsDate != null) dateFormat.format(todo.vcsDate) else "-"
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = FadeMissingRenderer
    }

    class DueDateColumn : TodoColumnInfo("Due Date") {
        override fun getTodoValue(todo: TodoItem): String = todo.dueDate?.toString() ?: "-"
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = DateRenderer
    }

    class TagsColumn : TodoColumnInfo("Tags") {
        override fun getTodoValue(todo: TodoItem): String = todo.tags.entries.filter { it.key != "due" }.joinToString(", ") { "${it.key}:${it.value}" }.ifEmpty { "-" }
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = TagsRenderer
    }

    class FileColumn : TodoColumnInfo("File") {
        override fun getTodoValue(todo: TodoItem): String = todo.getFileName()
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = DimmedRenderer
    }

    class LineColumn : TodoColumnInfo("Line") {
        override fun getTodoValue(todo: TodoItem): String = todo.lineNumber.toString()
        override fun getRenderer(item: DefaultMutableTreeNode): TableCellRenderer = DimmedRenderer
    }

    // --- Renderers ---

    object PriorityRenderer : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
            val priorityName = value?.toString() ?: "-"
            if (priorityName == "-" || priorityName.isEmpty()) {
                append("-", if (selected) SimpleTextAttributes.REGULAR_ATTRIBUTES else SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Gray._150))
                return
            }

            val iconSymbol = when (priorityName.uppercase()) {
                "CRITICAL" -> "🟣 "
                "HIGH" -> "🔴 "
                "MEDIUM" -> "🟠 "
                "LOW" -> "🟢 "
                else -> "🏷️ "
            }

            val color = com.todoplus.settings.TodoSettingsService.getInstance().getPriorityColor(priorityName)
            if (selected) {
                append("$iconSymbol$priorityName", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            } else if (color != null) {
                append("$iconSymbol$priorityName", SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, color))
            } else {
                append("$iconSymbol$priorityName", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }
        }
    }

    object FadeMissingRenderer : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
            val str = value?.toString() ?: "-"
            if (selected) {
                append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            } else if (str == "-") {
                append("-", SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Gray._150))
            } else {
                append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }

    object DateRenderer : ColoredTableCellRenderer() {
        private var cachedToday: java.time.LocalDate? = null
        private var cachedTodayTimestamp: Long = 0L

        private fun getToday(): java.time.LocalDate {
            val now = System.currentTimeMillis()
            if (cachedToday == null || (now - cachedTodayTimestamp) > 60_000L) {
                cachedToday = java.time.LocalDate.now()
                cachedTodayTimestamp = now
            }
            return cachedToday!!
        }

        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
            val str = value?.toString() ?: "-"
            if (selected) {
                append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                return
            }
            if (str == "-") {
                append("-", SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Gray._150))
                return
            }
            try {
                val date = java.time.LocalDate.parse(str)
                val today = getToday()
                when {
                    date.isBefore(today) -> append(str, SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, Color(220, 50, 50)))
                    date.isBefore(today.plusDays(7)) -> append(str, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color(220, 160, 30)))
                    else -> append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            } catch (e: Exception) {
                append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }

    object TagsRenderer : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
            val str = value?.toString() ?: "-"
            if (selected) {
                append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            } else if (str == "-") {
                append("-", SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, Gray._150))
            } else {
                append(str, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color(100, 150, 200))) 
            }
        }
    }

    object DimmedRenderer : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
            val str = value?.toString() ?: "-"
            if (selected) {
                append(str, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            } else {
                append(str, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Gray._120))
            }
        }
    }

    fun createColumns(): Array<ColumnInfo<*, *>> {
        return arrayOf(
            ItemColumn(),
            PriorityColumn(),
            AuthorColumn(),
            AssigneeColumn(),
            CategoryColumn(),
            IssueColumn(),
            DateAddedColumn(),
            DueDateColumn(),
            TagsColumn(),
            FileColumn(),
            LineColumn()
        )
    }
}
