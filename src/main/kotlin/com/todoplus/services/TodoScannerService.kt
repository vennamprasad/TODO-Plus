package com.todoplus.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.todoplus.models.TodoItem
import com.todoplus.parser.TodoParser
import com.intellij.openapi.application.runReadAction

/**
 * Service for scanning project files and extracting TODO items
 */
@Service(Service.Level.PROJECT)
class TodoScannerService(private val project: Project) {

    private val parser = TodoParser()

    /**
     * Scan all files in the project and extract TODO items
     */
    fun scanProject(): List<TodoItem> {
        val todos = mutableListOf<TodoItem>()
        
        // Get all files in project scope
        val files = findAllFiles()
        
        // Parse each file
        files.forEach { file ->
            // Check for cancellation
            com.intellij.openapi.progress.ProgressManager.checkCanceled()
            todos.addAll(scanFile(file))
        }
        
        return todos
    }

    /**
     * Scan a single file for TODO items
     * Uses PSI to read from editor buffer (unsaved changes) instead of disk
     */
    fun scanFile(file: VirtualFile): List<TodoItem> {
        if (!file.isValid || file.isDirectory) {
            return emptyList()
        }

        return try {
            runReadAction {
                // Try to get content from PSI (editor buffer) first
                val psiFile = PsiManager.getInstance(project).findFile(file)
                val content = if (psiFile != null) {
                    // Get from editor buffer (includes unsaved changes)
                    psiFile.text
                } else {
                    // Fallback to disk content
                    String(file.contentsToByteArray())
                }
                
                val lines = content.lines()
                parser.parseLines(lines, file.path)
            }
        } catch (e: Exception) {
            // Skip files that can't be read
            emptyList()
        }
    }

    /**
     * Find all relevant files in the project
     * Filters to only include source code files
     */
    private fun findAllFiles(): List<VirtualFile> {
        return runReadAction {
            val files = mutableListOf<VirtualFile>()
            val scope = GlobalSearchScope.projectScope(project)
            
            // Get common source file types
            val fileTypes = getSourceFileTypes()
            
            fileTypes.forEach { fileType ->
                val virtualFiles = FileTypeIndex.getFiles(fileType, scope)
                files.addAll(virtualFiles)
            }
            
            files
        }
    }

    /**
     * Get list of source code file types to scan
     */
    private fun getSourceFileTypes(): List<FileType> {
        val fileTypeManager = FileTypeManager.getInstance()
        val types = mutableListOf<FileType>()
        
        // Add common source file extensions
        val extensions = listOf(
            "java", "kt", "kts",  // Java, Kotlin
            "js", "ts", "jsx", "tsx",  // JavaScript, TypeScript
            "py",  // Python
            "go",  // Go
            "rs",  // Rust
            "cpp", "c", "h", "hpp",  // C/C++
            "cs",  // C#
            "swift",  // Swift
            "rb",  // Ruby
            "php",  // PHP
            "scala",  // Scala
            "groovy",  // Groovy
            "xml", "html"  // Markup
        )
        
        extensions.forEach { ext ->
            try {
                val fileType = fileTypeManager.getFileTypeByExtension(ext)
                if (!types.contains(fileType)) {
                    types.add(fileType)
                }
            } catch (ignored: Exception) {}
        }
        
        return types
    }

    /**
     * Get TODO statistics
     */
    fun getStatistics(todos: List<TodoItem>): TodoStatistics {
        return TodoStatistics(
            total = todos.size,
            highPriority = todos.count { it.priority == com.todoplus.models.Priority.HIGH },
            mediumPriority = todos.count { it.priority == com.todoplus.models.Priority.MEDIUM },
            lowPriority = todos.count { it.priority == com.todoplus.models.Priority.LOW },
            withAssignee = todos.count { it.assignee != null },
            withoutAssignee = todos.count { it.assignee == null }
        )
    }
}

/**
 * Statistics about scanned TODOs
 */
data class TodoStatistics(
    val total: Int,
    val highPriority: Int,
    val mediumPriority: Int,
    val lowPriority: Int,
    val withAssignee: Int,
    val withoutAssignee: Int
)
