package com.abonesma.core
import kotlin.random.Random
object FlashcardGenerator {
    fun generate(defs: List<Definition>, formulas: List<Formula>): List<Flashcard> =
        defs.map { Flashcard(it.term, it.meaning, "term") } + formulas.map { Flashcard("Formula: " + it.expr.substringBefore("=").trim(), it.expr, "formula") }
}
object QuestionGenerator {
    /** Builds only from extracted definitions. If source is too thin, returns fewer and says so in notes. */
    fun generate(defs: List<Definition>, mcqTarget: Int = 20, tfTarget: Int = 10, essayTarget: Int = 5, seed: Int = 1): QuestionBank {
        val rnd = Random(seed); val notes = mutableListOf<String>()
        val mcq = if (defs.size < 4) emptyList() else defs.shuffled(rnd).take(mcqTarget).map { d ->
            val wrong = defs.filter { it.term != d.term }.shuffled(rnd).take(3).map { it.term }
            val opts = (wrong + d.term).shuffled(rnd)
            Mcq("Which term matches: \"${d.meaning}\"?", opts, opts.indexOf(d.term), d.term, "${d.term}: ${d.meaning}")
        }
        val tf = if (defs.size < 2) emptyList() else defs.shuffled(rnd).take(tfTarget).mapIndexed { i, d ->
            if (i % 2 == 0) TrueFalse("${d.term}: ${d.meaning}", true, d.term, "${d.term}: ${d.meaning}")
            else { val o = defs.first { it.term != d.term && it.meaning != d.meaning }; TrueFalse("${d.term}: ${o.meaning}", false, d.term, "${d.term}: ${d.meaning}") }
        }
        val essay = defs.shuffled(rnd).take(essayTarget).map { Essay("Define and explain: ${it.term}", it.meaning) }
        if (mcq.size < mcqTarget) notes += "MCQ: ${mcq.size}/$mcqTarget (not enough verifiable source definitions)"
        if (tf.size < tfTarget) notes += "True/False: ${tf.size}/$tfTarget"
        if (essay.size < essayTarget) notes += "Essay: ${essay.size}/$essayTarget"
        return QuestionBank(mcq, tf, essay, notes)
    }
}
object ExamGrader {
    data class Result(val score: Int, val total: Int, val percent: Double, val wrongIdx: List<Int>)
    fun grade(correct: List<Int>, answers: Map<Int, Int>): Result {
        val wrong = correct.indices.filter { answers[it] != correct[it] }
        val s = correct.size - wrong.size
        return Result(s, correct.size, if (correct.isEmpty()) 0.0 else s * 100.0 / correct.size, wrong)
    }
}
