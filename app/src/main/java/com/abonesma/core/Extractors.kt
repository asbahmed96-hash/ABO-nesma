package com.abonesma.core
object FormulaExtractor {
    private val re = Regex("""^[^=<>]{1,40}=[^=]{1,80}$""")
    fun extract(text: String): List<Formula> = text.lines().map { it.trim() }
        .filter { it.length in 5..120 && re.matches(it) && it.any { c -> c.isLetter() } && it.count { c -> c == ' ' } <= 12 && !it.endsWith(".") }
        .distinct().map { Formula(it, it) }
}
object DefinitionExtractor {
    private val patterns = listOf(
        Regex("""^(.{2,50}?)\s+(?:is defined as|is called|refers to|means)\s+(.{10,300})$""", RegexOption.IGNORE_CASE),
        Regex("""^(.{2,50}?)\s*:\s+(.{15,300})$"""),
        Regex("""^(.{2,40}?)\s+(?:هو|هي|يُعرَّف بأنه|يعرف بأنه|تعرف بأنها|تُعرَّف بأنها)\s+(.{10,300})$"""))
    fun extract(text: String): List<Definition> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        .flatMap { line -> listOf(line) + (if (line.length > 120) TextUtils.sentences(line) else emptyList()) }
        .mapNotNull { s -> patterns.firstNotNullOfOrNull { it.find(s.trimEnd('.')) } }
        .map { Definition(it.groupValues[1].trim(), it.groupValues[2].trim()) }
        .filter { it.term.split(" ").size <= 6 }.distinctBy { it.term.lowercase() }
}
object ComparisonExtractor {
    private val re = Regex("""^(.{10,200}?),?\s+(?:whereas|while|in contrast|unlike|بينما|على عكس)\s+(.{10,200})$""", RegexOption.IGNORE_CASE)
    fun extract(text: String): List<Comparison> = TextUtils.sentences(text).mapNotNull { s ->
        re.find(s.trimEnd('.'))?.let { Comparison(it.groupValues[1].trim(), it.groupValues[2].trim(), s) }
    }
}
