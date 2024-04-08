package com.github.yifei0727.idea.plugin.completion.suggestion

import com.github.yifei0727.idea.plugin.completion.utils.StringUtil
import com.intellij.openapi.diagnostic.thisLogger
import java.security.SecureRandom
import kotlin.math.pow

// 当需要输入的类型为 int 时时，自动获取当前文件中的所有 errno 值 然后生成一个新的 提示
object NumberSuggest {
    /* 最佳的匹配方式是 每个文件头部都有一个类似  文件编号建议固定4位数字
     *   file# 0012
     *   // 0013
     *   之类的格式注释 用来标记当前文件的 errno 值的起始值
     */
    private val regex = Regex(".*[fF][iI][lL][eE]_?(([iI][dD])|([nN][oO]))?[#:/*]+\\s*([0-9]{3,4})\\s*$")

    private fun scanPrefixNumTagFromCode(currentCodeText: String): String? {
        return currentCodeText.split("\n")
            .map { it.trim() }
            .firstOrNull { regex.matches(it) }
            ?.let { regex.find(it)?.groups?.get(4)?.value }
    }

    fun suggest(text: String): List<String> {
        val s = ArrayList<String>()
        s.addAll(scanAndFormatErrnoThenReGenerate(text, 3))
        s.addAll(scanAndFormatErrnoThenReGenerate(text, 4))
        return s
    }

    /**
     * 生成一个新的 errno 值 nnn_nnn 格式
     * 生成一个新的 errno 值 nnnn_nnnn 格式
     * @param text 当前文件的文本内容
     * @param length 3/4
     * @return 生成的新的 errno 值
     */
    private fun scanAndFormatErrnoThenReGenerate(text: String, length: Int): List<String> {
        if (length !in 3..7) {
            thisLogger().error("length must be 3, 4, 5, 6, 7")
            return emptyList()
        }
        // 读取当前文件中的所有 errno 值
        val patternString = "[^0-9]+([0-9]{$length}_[0-9]{$length})[^0-9]*"
        val regex = Regex(patternString)
        val matchResult = text.split("\n").flatMap { it ->//regex.findAll(text).map { it.groups[1] }.map { it?.value }
            regex.findAll(it)
        }.map { it.groups[1]?.value }
        // 去重复
        val errnoList = matchResult.distinct()
        // 排序
        val errnoListSorted = errnoList.sortedWith(compareBy { it }).toList()
        // 提取前n位 nnnn_
        val errnoListSortedPrefix = errnoListSorted.map { it?.substring(0, length) }.distinct()
            .flatMap {
                if (it.isNullOrBlank()) {
                    emptyList()
                } else {
                    listOf(it)
                }
            }
        val retList = mutableListOf<String>()

        if (errnoListSortedPrefix.isEmpty()) {
            val prefix = scanPrefixNumTagFromCode(text)
            if (!prefix.isNullOrBlank() && prefix.length == length) {
                generateSuffix(emptySequence(), length, true)?.let {
                    retList.add(prefix + "_" + StringUtil.padLeftZero(it, length))
                }
                generateSuffix(emptySequence(), length, false)?.let {
                    retList.add(prefix + "_" + StringUtil.padLeftZero(it, length))
                }
            }
        }
//        else if (errnoListSortedPrefix.count() == 1) {
//            // good 说明当前文件中的 errno 值都是同一个系列的
//            // 生成一个新的提示
//            val errnoPrefix = errnoListSortedPrefix.first()
//            val errnoListSortedSuffix = errnoListSorted.map { it?.substring(5, 9) }.distinct().map { it?.toInt() }
//            var eid = 1
//            while (eid in errnoListSortedSuffix) {
//                eid++
//            }
//            if (eid > 9999) {
//                return emptyList()
//            }
//            val errnoSuffix = eid.toString().padStart(4, '0')
//            val errno = errnoPrefix + "_" + errnoSuffix
//            return listOf(errno)
//        }
        else {
            // 说明当前文件中的 errno 值不是同一个系列(有多种)
            // 生成一个新的提示
            // 生成对应头的提示
            for (prefix: String in errnoListSortedPrefix) {
                val errnoListSortedSuffix = errnoListSorted
                    .asSequence()
                    .filter { !it.isNullOrBlank() && it.startsWith(prefix) }
                    .map { it?.substring(length + 1, 2 * length + 1) }
                    .filterNotNull()
                    .distinct()
                    .map { it.toInt() }
                // 生成1个 顺序的eid 和一个随机的eid
                generateSuffix(errnoListSortedSuffix, length, true)?.let {
                    retList.add(prefix + "_" + StringUtil.padLeftZero(it, length))
                }
                generateSuffix(errnoListSortedSuffix, length, false)?.let {
                    retList.add(prefix + "_" + StringUtil.padLeftZero(it, length))
                }
            }
        }
        return retList.distinct()
    }

    private fun generateSuffix(usedErrnoList: Sequence<Int>, maxLen: Int, ordered: Boolean): Int? {
        val maxTries: Int = 10.0.pow(maxLen).toInt()
        var eid: Int = if (ordered) 1 else SecureRandom().nextInt(1, maxTries)
        for (noUsed in 1..maxTries) {
            if (eid !in usedErrnoList) {
                return eid
            }
            eid++
        }
        if (eid in usedErrnoList) {
            return null
        }
        return eid
    }
}
