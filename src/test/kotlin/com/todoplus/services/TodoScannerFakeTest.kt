package com.todoplus.services

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoScannerFakeTest {

    // Simulating the exact path filtration logic inside TodoScannerService
    private fun isPathIgnored(path: String, ignoredDirs: List<String>): Boolean {
        val normalizedPath = path.replace('\\', '/')
        return ignoredDirs.any { ignoredDir ->
            normalizedPath.contains("/$ignoredDir/") || normalizedPath.endsWith("/$ignoredDir")
        }
    }

    @Test
    fun `test ignored directories path filtering`() {
        val ignoredDirs = listOf("build", "node_modules", ".idea", ".next", "coverage", ".venv")

        // Should be ignored (Unix style)
        assertTrue(isPathIgnored("/Users/project/build/classes/main.kt", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/node_modules/library/index.js", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/.idea/workspace.xml", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/build", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/.next/static/chunks/main.js", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/coverage/lcov-report/index.html", ignoredDirs))
        assertTrue(isPathIgnored("/Users/project/.venv/lib/site-packages/pkg.py", ignoredDirs))

        // Should be ignored (Windows style backslashes)
        assertTrue(isPathIgnored("C:\\Users\\project\\build\\classes\\main.kt", ignoredDirs))
        assertTrue(isPathIgnored("C:\\Users\\project\\node_modules\\library\\index.js", ignoredDirs))

        // Should NOT be ignored (substring matches that aren't exact directories should be safe)
        assertFalse(isPathIgnored("/Users/project/build_scripts/script.sh", ignoredDirs))
        assertFalse(isPathIgnored("/Users/project/src/my_node_modules.js", ignoredDirs))
        assertFalse(isPathIgnored("/Users/project/src/main.kt", ignoredDirs))
    }
}
