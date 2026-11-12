package com.abonesma.core
data class Lesson(val title: String, val text: String)
data class Chapter(val title: String, val lessons: List<Lesson>)
data class Formula(val expr: String, val source: String)
data class Definition(val term: String, val meaning: String, val lesson: String = "")
data class Comparison(val a: String, val b: String, val source: String)
data class Flashcard(val front: String, val back: String, val kind: String)
data class Mcq(val q: String, val options: List<String>, val correct: Int, val source: String, val explain: String = "")
data class TrueFalse(val statement: String, val answer: Boolean, val source: String, val explain: String = "")
data class Essay(val q: String, val reference: String)
data class QuestionBank(val mcq: List<Mcq>, val tf: List<TrueFalse>, val essay: List<Essay>, val notes: List<String>)
data class Q(val text: String, val options: List<String>, val correct: Int, val source: String, val explain: String)
data class ChapterContent(
    val title: String, val summaries: List<Pair<String, List<String>>>, val formulas: List<Formula>,
    val defs: List<Definition>, val comps: List<Comparison>, val cards: List<Flashcard>, val bank: QuestionBank)
