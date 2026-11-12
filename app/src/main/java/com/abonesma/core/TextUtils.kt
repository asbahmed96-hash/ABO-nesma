package com.abonesma.core
object TextUtils {
    fun sentences(text: String): List<String> =
        text.replace("\n", " ").split(Regex("(?<=[.!?؟])\\s+")).map { it.trim() }.filter { it.length > 15 }
    fun words(s: String): List<String> = Regex("[\\p{L}\\p{N}]+").findAll(s.lowercase()).map { it.value }.toList()
    val stop = setOf("the","a","an","of","to","and","in","is","are","for","on","with","that","this","by","as","it","من","في","على","إلى","عن","هو","هي","أن","ان","التي","الذي","مع")
}
