package com.payfacto.braingames.feature.wordsearch.data

import android.content.Context
import com.payfacto.braingames.R

object WordBank {
    fun getWords(context: Context, maxLength: Int): List<String> {
        val all = context.resources.getStringArray(R.array.wordsearch_words)
        return all.filter { it.length <= maxLength }.shuffled()
    }
}
