package com.todoplus.settings

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodoSettingsTest {

    @Test
    fun `test default settings`() {
        val service = TodoSettingsService()
        val state = service.state

        // Verify default issue pattern
        assertEquals("[A-Z]+-\\d+", state.issuePattern)
        
        // Verify default URL template is empty
        assertEquals("", state.issueUrlTemplate)
        
        // Verify default priorities exist
        assertEquals(3, state.priorities.size)
        assertTrue(state.priorities.any { it.name == "HIGH" })
        assertTrue(state.priorities.any { it.name == "MEDIUM" })
        assertTrue(state.priorities.any { it.name == "LOW" })
    }

    @Test
    fun `test modify settings`() {
        val service = TodoSettingsService()
        val state = service.state

        state.issueUrlTemplate = "https://github.com/user/repo/issues/{id}"
        state.issuePattern = "ISSUE-\\d+"
        
        assertEquals("https://github.com/user/repo/issues/{id}", state.issueUrlTemplate)
        assertEquals("ISSUE-\\d+", state.issuePattern)
    }
}
