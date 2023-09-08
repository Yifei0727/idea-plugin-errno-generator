package com.github.yifei0727.idea.plugin.completion.utils

object StringUtil {
    fun padLeftZero(num: Int, length: Int): String {
        return num.toString().padStart(length, '0')
    }
}