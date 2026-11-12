package com.abonesma.core
import org.json.JSONArray
import org.json.JSONObject
object ContentBuilder {
    fun build(title: String, lessons: List<Lesson>): ChapterContent {
        val all = lessons.joinToString("\n") { it.text }
        val defs = lessons.flatMap { l -> DefinitionExtractor.extract(l.text).map { it.copy(lesson = l.title) } }.distinctBy { it.term.lowercase() }
        val formulas = lessons.flatMap { l -> FormulaExtractor.extract(l.text).map { it.copy(source = l.title) } }.distinctBy { it.expr }
        val comps = ComparisonExtractor.extract(all)
        return ChapterContent(title, lessons.map { it.title to Summarizer.summarize(it.text) }, formulas, defs, comps,
            FlashcardGenerator.generate(defs, formulas), QuestionGenerator.generate(defs))
    }
    fun objective(b: QuestionBank, trueLabel: String = "True", falseLabel: String = "False"): List<Q> =
        b.mcq.map { Q(it.q, it.options, it.correct, it.source, it.explain) } +
        b.tf.map { Q(it.statement, listOf(trueLabel, falseLabel), if (it.answer) 0 else 1, it.source, it.explain) }
}
object ChapterExporter {
    fun txt(c: ChapterContent): String = buildString {
        appendLine("== ${c.title} ==\n")
        appendLine("## Summaries"); c.summaries.forEach { (t, s) -> appendLine("- $t"); s.forEach { appendLine("    * $it") } }
        appendLine("\n## Formulas"); c.formulas.forEach { appendLine("- ${it.expr}  [${it.source}]") }
        appendLine("\n## Definitions"); c.defs.forEach { appendLine("- ${it.term}: ${it.meaning}  [${it.lesson}]") }
        appendLine("\n## Comparisons"); c.comps.forEach { appendLine("- ${it.a}  <->  ${it.b}") }
        appendLine("\n## Flashcards"); c.cards.forEach { appendLine("- ${it.front} => ${it.back}") }
        appendLine("\n## MCQ"); appendLine(Exporter.mcqTxt(c.bank.mcq))
        appendLine("\n## True/False"); c.bank.tf.forEach { appendLine("- ${it.statement} -> ${it.answer}") }
        appendLine("\n## Essay"); c.bank.essay.forEach { appendLine("- ${it.q}\n    ref: ${it.reference}") }
        c.bank.notes.forEach { appendLine("NOTE: $it") }
    }
    fun json(c: ChapterContent): String = JSONObject().apply {
        put("title", c.title)
        put("summaries", JSONArray(c.summaries.map { JSONObject().put("lesson", it.first).put("sentences", JSONArray(it.second)) }))
        put("formulas", JSONArray(c.formulas.map { JSONObject().put("expr", it.expr).put("lesson", it.source) }))
        put("definitions", JSONArray(c.defs.map { JSONObject().put("term", it.term).put("meaning", it.meaning).put("lesson", it.lesson) }))
        put("comparisons", JSONArray(c.comps.map { JSONObject().put("a", it.a).put("b", it.b) }))
        put("flashcards", JSONArray(c.cards.map { JSONObject().put("front", it.front).put("back", it.back).put("kind", it.kind) }))
        put("mcq", JSONArray(Exporter.mcqJson(c.bank.mcq)))
        put("trueFalse", JSONArray(c.bank.tf.map { JSONObject().put("statement", it.statement).put("answer", it.answer) }))
        put("essay", JSONArray(c.bank.essay.map { JSONObject().put("q", it.q).put("reference", it.reference) }))
        put("notes", JSONArray(c.bank.notes))
    }.toString(2)
}
