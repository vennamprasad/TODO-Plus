package com.todoplus.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.todoplus.models.TodoItem
import com.todoplus.parser.TodoParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.roots.ProjectFileIndex

/**
 * Service for scanning project files and extracting TODO items
 */
@Service(Service.Level.PROJECT)
class TodoScannerService(private val project: Project) {

    companion object {
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(TodoScannerService::class.java)
    }

    private fun createParser(): TodoParser {
        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
        return TodoParser(settings.getState().issuePattern)
    }

    /**
     * Scan all files in the project and extract TODO items
     */
    fun scanProject(indicator: ProgressIndicator? = null): List<TodoItem> {
        LOG.info("Starting project scan for TODOs")
        val todos = mutableListOf<TodoItem>()
        
        // Get all files in project scope
        val files = findAllFiles()
        LOG.info("Found ${files.size} files to scan")
        
        val totalFiles = files.size
        if (indicator != null && totalFiles > 0) {
            indicator.isIndeterminate = false
        }
        
        // Parse each file
        files.forEachIndexed { index, file ->
            // Check for cancellation
            com.intellij.openapi.progress.ProgressManager.checkCanceled()
            
            if (indicator != null && totalFiles > 0) {
                indicator.fraction = (index + 1).toDouble() / totalFiles
                indicator.text2 = "Scanning file ${index + 1} of $totalFiles: ${file.name}"
            }
            
            todos.addAll(scanFile(file))
        }
        
        LOG.info("Project scan completed. Found ${todos.size} TODO items")
        return todos
    }

    /**
     * Scan a single file for TODO items
     * Uses PSI to read from editor buffer (unsaved changes) instead of disk
     */
    fun scanFile(file: VirtualFile): List<TodoItem> {
        val maxMb = com.todoplus.settings.TodoSettingsService.getInstance().getState().maxFileSizeMb
        val maxSizeBytes = maxMb * 1024L * 1024L
        if (!file.isValid || file.isDirectory || file.length > maxSizeBytes) {
            return emptyList()
        }

        return try {
            val parsedTodos = ApplicationManager.getApplication().runReadAction(Computable {
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
                val parser = createParser()
                parser.parseLines(lines, file.path)
            })
                
            // Fetch VCS data OUTSIDE of the ReadAction lock to prevent 'Synchronous execution under ReadAction' errors
            val vcsService = project.service<com.todoplus.services.vcs.TodoVcsService>()
            parsedTodos.forEach { todo ->
                vcsService.getVcsDataForTodo(todo, file)
            }
            
            parsedTodos
        } catch (e: Exception) {
            // Log warning instead of swallowing
            LOG.warn("Failed to scan file: ${file.path}", e)
            emptyList()
        }
    }

    /**
     * Find all relevant files in the project
     * Filters to only include source code files
     */
    private fun findAllFiles(): List<VirtualFile> {
        return ApplicationManager.getApplication().runReadAction(Computable {
            val files = mutableListOf<VirtualFile>()
            val scope = GlobalSearchScope.projectScope(project)
            val fileIndex = ProjectFileIndex.getInstance(project)
            
            // Get ignored directories
            val ignoredDirs = com.todoplus.settings.TodoSettingsService.getInstance().getIgnoredDirectories()
            
            // Get common source file types
            val fileTypes = getSourceFileTypes()
            
            fileTypes.forEach { fileType ->
                val virtualFiles = FileTypeIndex.getFiles(fileType, scope)
                // Filter out files in ignored directories or excluded by IDE index
                val filteredFiles = virtualFiles.filter { file ->
                    // 1. Skip if IDE index marks file/directory as excluded
                    if (fileIndex.isExcluded(file)) return@filter false
                    
                    // 2. Normalize path separators for cross-platform compatibility
                    val path = file.path.replace('\\', '/')
                    
                    // 3. Check against user-configured ignored directory rules
                    !ignoredDirs.any { ignoredDir ->
                        path.contains("/$ignoredDir/") || path.endsWith("/$ignoredDir")
                    }
                }
                files.addAll(filteredFiles)
            }
            
            files
        })
    }

    /**
     * Get list of source code file types to scan
     */
    private fun getSourceFileTypes(): List<FileType> {
        val fileTypeManager = FileTypeManager.getInstance()
        val types = mutableListOf<FileType>()
        
        // Add common source file extensions
        val extensions = listOf(
            "java", "kt", "kts", "cs", "cshtml", "razor", "fs", "fsx", "scala", "groovy", "gradle",  // JVM & .NET
            "js", "ts", "jsx", "tsx", "vue", "svelte", "astro", "html", "htm", "css", "scss", "less", "sass",  // Web & Frontend
            "cpp", "c", "h", "hpp", "cc", "cxx", "rs", "go", "swift", "m", "mm",  // Systems & Native
            "py", "rb", "php", "blade.php", "erb", "sh", "bash", "zsh", "ps1", "lua", "dart", "ex", "exs", "clj",  // Scripting & Mobile
            "xml", "sql", "yaml", "yml", "json", "toml", "properties", "ini", "conf", "env", "md", "mdx", "rst"  // Config & Markup
        )
        
        extensions.forEach { ext ->
            try {
                val fileType = fileTypeManager.getFileTypeByExtension(ext)
                if (!types.contains(fileType)) {
                    types.add(fileType)
                }
            } catch (ignored: Exception) {
                // Ignore unknown file types
            }
        }
        
        // Include all registered non-binary text file types available in the IDE runtime (e.g. Rider / C# / WebStorm / PyCharm)
        try {
            fileTypeManager.registeredFileTypes.forEach { ft ->
                if (!ft.isBinary && !types.contains(ft) && ft.name.uppercase() != "UNKNOWN") {
                    types.add(ft)
                }
            }
        } catch (ignored: Exception) {
        }
        
        return types
    }

    /**
     * Get TODO statistics
     */
    fun getStatistics(todos: List<TodoItem>): TodoStatistics {
        return TodoStatistics(
            total = todos.size,
            criticalPriority = todos.count { it.priority == com.todoplus.models.Priority.CRITICAL },
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
    val criticalPriority: Int,
    val highPriority: Int,
    val mediumPriority: Int,
    val lowPriority: Int,
    val withAssignee: Int,
    val withoutAssignee: Int
)
