package com.todoplus.parser

import com.todoplus.models.Priority
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TodoParserTest {

    private val parser = TodoParser()

    @Test
    fun `parse simple TODO`() {
        val line = "// TODO: Fix this bug"
        val result = parser.parseLine(line, "test.kt", 10)

        assertNotNull(result)
        assertEquals("Fix this bug", result.description)
        assertNull(result.assignee)
        assertNull(result.priority)
        assertNull(result.category)
        assertEquals(10, result.lineNumber)
    }

    @Test
    fun `parse TODO with assignee`() {
        val line = "// TODO(@john): Implement feature"
        val result = parser.parseLine(line, "test.kt", 20)

        assertNotNull(result)
        assertEquals("Implement feature", result.description)
        assertEquals("john", result.assignee)
        assertNull(result.priority)
        assertNull(result.category)
    }

    @Test
    fun `parse TODO with priority`() {
        val line = "// TODO(priority:high): Critical fix needed"
        val result = parser.parseLine(line, "test.kt", 30)

        assertNotNull(result)
        assertEquals("Critical fix needed", result.description)
        assertNull(result.assignee)
        assertEquals(Priority.HIGH, result.priority)
        assertNull(result.category)
    }

    @Test
    fun `parse TODO with category`() {
        val line = "// TODO(category:bug): Memory leak"
        val result = parser.parseLine(line, "test.kt", 40)

        assertNotNull(result)
        assertEquals("Memory leak", result.description)
        assertNull(result.assignee)
        assertNull(result.priority)
        assertEquals("bug", result.category)
    }

    @Test
    fun `parse TODO with all metadata`() {
        val line = "// TODO(@sarah priority:medium category:refactor): Clean up code"
        val result = parser.parseLine(line, "test.kt", 50)

        assertNotNull(result)
        assertEquals("Clean up code", result.description)
        assertEquals("sarah", result.assignee)
        assertEquals(Priority.MEDIUM, result.priority)
        assertEquals("refactor", result.category)
    }

    @Test
    fun `parse TODO case insensitive`() {
        val line = "// todo(PRIORITY:HIGH): Case test"
        val result = parser.parseLine(line, "test.kt", 60)

        assertNotNull(result)
        assertEquals("Case test", result.description)
        assertEquals(Priority.HIGH, result.priority)
    }

    @Test
    fun `parse TODO with extra spaces`() {
        val line = "//   TODO   (  @mike  priority:low  )  :   Spacing test"
        val result = parser.parseLine(line, "test.kt", 70)

        assertNotNull(result)
        assertEquals("Spacing test", result.description)
        assertEquals("mike", result.assignee)
        assertEquals(Priority.LOW, result.priority)
    }

    @Test
    fun `non-TODO line returns null`() {
        val line = "// This is just a regular comment"
        val result = parser.parseLine(line, "test.kt", 80)

        assertNull(result)
    }

    @Test
    fun `parse multiple lines`() {
        val lines = listOf(
            "package com.example",
            "// TODO(@john priority:high): Fix login",
            "class Example {",
            "// TODO(category:feature): Add dark mode",
            "    fun test() {}",
            "// TODO: Simple todo",
            "}"
        )

        val results = parser.parseLines(lines, "Example.kt")

        assertEquals(3, results.size)
        assertEquals("Fix login", results[0].description)
        assertEquals("Add dark mode", results[1].description)
        assertEquals("Simple todo", results[2].description)
    }

    @Test
    fun `test parse with tags`() {
        val line = "// TODO(risk:high estimate:3d): Complex todo"
        val result = parser.parseLine(line, "test.kt", 1)

        assertNotNull(result)
        assertEquals("Complex todo", result.description)
        assertEquals("high", result.tags["risk"])
        assertEquals("3d", result.tags["estimate"])
        assertNull(result.priority)
        assertNull(result.assignee)
    }

    @Test
    fun `test parse mixed standard and custom tags`() {
        val line = "// TODO(@me priority:high type:bug risk:critical): Mixed todo"
        val result = parser.parseLine(line, "test.kt", 1)

        assertNotNull(result)
        assertEquals("Mixed todo", result.description)
        assertEquals("me", result.assignee)
        assertEquals(Priority.HIGH, result.priority)
        
        assertEquals("bug", result.tags["type"])
        assertEquals("critical", result.tags["risk"])
    }

    @Test
    fun `test parse with due date`() {
        val today = java.time.LocalDate.now()
        val line = "// TODO(due:today): Finish this today"
        val result = parser.parseLine(line, "test.kt", 1)

        assertNotNull(result)
        assertEquals(today, result.dueDate)
        
        val specificDate = java.time.LocalDate.parse("2023-12-25")
        val line2 = "// TODO(due:2023-12-25): Christmas task"
        val result2 = parser.parseLine(line2, "test.kt", 2)
        assertNotNull(result2)
        assertEquals(specificDate, result2.dueDate)
    }

    @Test
    fun `test parse with issue tag`() {
        val line = "// TODO(issue:PROJ-123): Fix bug"
        val result = parser.parseLine(line, "test.kt", 1)

        assertNotNull(result)
        assertEquals("PROJ-123", result.issueId)
    }

    @Test
    fun `test parse with issue regex`() {
        // Mock settings would be ideal here, but since we rely on the service instance which might not be mocked easily in unit tests without a refined architecture,
        // we might depend on default settings or we interpret the default behavior.
        // The default pattern is [A-Z]+-\d+
        
        // For this test, we instantiate a parser with a known pattern
        val parserWithRegex = TodoParser("[A-Z]+-\\d+")
        
        val line = "// TODO: Fix bug related to PROJ-456"
        val result = parserWithRegex.parseLine(line, "test.kt", 1)

        assertNotNull(result)
        assertEquals("PROJ-456", result.issueId) 
    }
    @Test
    fun `test parse with empty regex`() {
        val parserEmpty = TodoParser("")
        val line = "// TODO: Fix bug PROJ-123"
        val result = parserEmpty.parseLine(line, "test.kt", 1)

        assertNotNull(result)
        assertNull(result.issueId)
    }
}
