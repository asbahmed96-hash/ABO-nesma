package com.abonesma.core
object Summarizer {
    /** Extractive: picks highest-scoring existing sentences; never invents text. */
    fun summarize(text: String, maxSentences: Int = 5): List<String> {
        val sents = TextUtils.sentences(text)
        if (sents.size <= maxSentences) return sents
        val freq = HashMap<String, Int>()
        sents.forEach { s -> TextUtils.words(s).filter { it !in TextUtils.stop && it.length > 2 }.forEach { freq[it] = (freq[it] ?: 0) + 1 } }
        val scored = sents.mapIndexed { i, s ->
            val w = TextUtils.words(s).filter { it !in TextUtils.stop }
            i to (if (w.isEmpty()) 0.0 else w.sumOf { (freq[it] ?: 0).toDouble() } / w.size)
        }
        return scored.sortedByDescending { it.second }.take(maxSentences).map { it.first }.sorted().map { sents[it] }
    }
}
