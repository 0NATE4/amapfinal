package com.example.amap.util

import com.huaban.analysis.jieba.JiebaSegmenter
import net.sourceforge.pinyin4j.PinyinHelper

object PinyinUtil {
    private val segmenter = JiebaSegmenter()

    fun toPinyin(text: String): String {
        val words = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH)
        return words.joinToString(" ") { wordSeg ->
            val word = wordSeg.word
            word.toCharArray().joinToString("") { char ->
                val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(char)
                if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                    val pinyin = pinyinArray[0].replace(Regex("\\d"), "")
                    pinyin.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    char.toString()
                }
            }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.replace(Regex(" +"), " ").trim()
    }
} 