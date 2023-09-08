package com.github.yifei0727.idea.plugin.completion.suggestion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class ErrnoCompletionHelper : CompletionContributor() {
    init {
        extend(
            CompletionType.SMART, // 智能补全，自动类型匹配 int）
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    parameters.editor.document.getText(TextRange(0, parameters.editor.document.textLength)).let { it ->
                        // 目前仅支持数字类型的提示 最多建议 6个
                        NumberSuggest.suggest(it).take(6).forEach {
                            resultSet.addElement(LookupElementBuilder.create(it))
                        }
                    }
                }
            }
        )
    }


}
