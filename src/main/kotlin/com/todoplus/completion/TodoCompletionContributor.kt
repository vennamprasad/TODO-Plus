package com.todoplus.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiComment
import com.intellij.util.ProcessingContext

/**
 * Provides completion for TODO keys and values
 */
class TodoCompletionContributor : CompletionContributor() {

    private val TODO_KEYS = listOf(
        "priority", "assignee", "category", // Standard
        "due", "estimate", "risk"           // Common custom
    )
    
    // private val PRIORITIES = listOf("HIGH", "MEDIUM", "LOW") - Now fetched dynamically
    private val RISKS = listOf("high", "medium", "low", "critical")

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inside(PsiComment::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    val element = parameters.position
                    val text = element.text
                    
                    // Simple check if we are in a TODO comment
                    // Note: deeper PSI analysis would be better but simple string check works for generic comments
                    if (!text.contains("TODO", ignoreCase = true)) return

                    // Check if we are inside the parentheses: TODO(...)
                    // This is a rough check, getting exact offset relative to TODO start is better
                    // but for a generic contributor this is a start.
                    
                    // Suggest keys
                    TODO_KEYS.forEach { key ->
                        resultSet.addElement(LookupElementBuilder.create("$key:"))
                    }
                    
                    // Suggest values for specific keys if the prefix matches
                    val prefix = resultSet.prefixMatcher.prefix
                     if (prefix.startsWith("priority:", ignoreCase = true)) {
                        val settings = com.todoplus.settings.TodoSettingsService.getInstance()
                        settings.getPriorities().forEach { p ->
                            resultSet.addElement(LookupElementBuilder.create("priority:${p.name.lowercase()}"))
                        }
                    } else if (prefix.startsWith("risk:", ignoreCase = true)) {
                        RISKS.forEach { r ->
                             resultSet.addElement(LookupElementBuilder.create("risk:$r"))
                        }
                    } else if (prefix.startsWith("due:", ignoreCase = true)) {
                        resultSet.addElement(LookupElementBuilder.create("due:today").withTypeText("YYYY-MM-DD"))
                        resultSet.addElement(LookupElementBuilder.create("due:tomorrow").withTypeText("YYYY-MM-DD"))
                        val nextFriday = java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.FRIDAY))
                        resultSet.addElement(LookupElementBuilder.create("due:$nextFriday").withTypeText("Next Friday"))
                    } else if (prefix.startsWith("issue:", ignoreCase = true)) {
                        resultSet.addElement(LookupElementBuilder.create("issue:").withTypeText("Issue ID"))
                    }
                }
            }
        )
    }
}
