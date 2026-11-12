package com.abonesma.core
object ChapterDetector {
    private val chapterRe = Regex("""^\s*(chapter\s+\S+.*|unit\s+\S+.*|الفصل\s+\S+.*|الوحدة\s+\S+.*)$""", RegexOption.IGNORE_CASE)
    private val lessonRe = Regex("""^\s*(lesson\s+\S+.*|section\s+\d+(\.\d+)*.*|\d+\.\d+\s+\S+.*|الدرس\s+\S+.*)$""", RegexOption.IGNORE_CASE)
    fun detect(text: String, bookTitle: String = "Book"): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        var cTitle: String? = null
        val lessons = mutableListOf<Lesson>()
        var lTitle = "Introduction"
        val buf = StringBuilder()
        fun flushLesson() { if (buf.isNotBlank()) lessons += Lesson(lTitle, buf.toString().trim()); buf.clear() }
        fun flushChapter() { flushLesson(); if (lessons.isNotEmpty()) chapters += Chapter(cTitle ?: bookTitle, lessons.toList()); lessons.clear() }
        for (line in text.lines()) {
            val t = line.trim()
            when {
                t.length in 3..100 && chapterRe.matches(t) -> { flushChapter(); cTitle = t; lTitle = "Introduction" }
                t.length in 3..100 && lessonRe.matches(t) -> { flushLesson(); lTitle = t }
                else -> buf.appendLine(line)
            }
        }
        flushChapter()
        return chapters
    }
}
