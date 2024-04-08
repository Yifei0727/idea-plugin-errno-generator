package com.github.yifei0727.idea.plugin.completion.suggestion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiIdentifier
import com.intellij.util.ProcessingContext

class ErrnoCompletionHelper : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC, // 智能补全，自动类型匹配 int/long/uint/ulong/
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                public override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    // todo 如果当前需要提供一个 number 类型建议，则提供否则返回空

                    if (parameters.position is PsiIdentifier) {
                        parameters.editor.document.getText(TextRange(0, parameters.originalFile.textLength))
                            .let { it ->
                                // 目前仅支持数字类型的提示 最多建议 6个
                                NumberSuggest.suggest(it).take(6).forEach {
                                    resultSet.addElement(LookupElementBuilder.create(it))
                                }
                            }
                    } else {
                        thisLogger().warn("not support type maybe need add later " + parameters.position)
                    }
                }
            }
        )
    }


}
