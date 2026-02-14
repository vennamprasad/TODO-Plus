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
}
