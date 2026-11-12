package com.abonesma.core
/** Offline dictionary/phrase translator. Word-level only: NOT professional translation. */
class Translator(entries: List<Pair<String, String>>) {
    private val en2ar = HashMap<String, String>()
    private val ar2en = HashMap<String, String>()
    init { for ((e, a) in entries) { en2ar[e.lowercase()] = a; if (!ar2en.containsKey(a)) ar2en[a] = e } }
    companion object {
        fun parse(tsv: String) = Translator(tsv.lines().map { it.split("\t") }.filter { it.size >= 2 && it[0].isNotBlank() }.map { it[0].trim() to it[1].trim() })
        fun isArabic(s: String): Boolean {
            val l = s.filter { it.isLetter() }; if (l.isEmpty()) return false
            return l.count { it in '\u0600'..'\u06FF' } * 2 > l.length
        }
    }
    fun auto(text: String) = translate(text, !isArabic(text))
    fun translate(text: String, toArabic: Boolean): String {
        val map = if (toArabic) en2ar else ar2en
        val toks = Regex("[\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+").findAll(text).map { it.value }.toList()
        val sb = StringBuilder(); var i = 0
        fun isWord(s: String) = s[0].isLetterOrDigit()
        while (i < toks.size) {
            val tk = toks[i]
            if (!isWord(tk)) { sb.append(tk); i++; continue }
            var done = false
            for (n in 3 downTo 1) {
                var j = i; var cnt = 1; val parts = mutableListOf(tk)
                while (cnt < n && j + 2 < toks.size && toks[j + 1] == " " && isWord(toks[j + 2])) { parts.add(toks[j + 2]); j += 2; cnt++ }
                if (cnt < n) continue
                val tr = lookup(map, parts.joinToString(" "), toArabic)
                if (tr != null) { sb.append(tr); i = j + 1; done = true; break }
            }
            if (!done) { sb.append(tk); i++ }
        }
        return sb.toString()
    }
    private fun lookup(map: Map<String, String>, p: String, toArabic: Boolean): String? {
        val k = p.lowercase()
        map[k]?.let { return it }
        if (toArabic) {
            if (k.endsWith("es")) map[k.dropLast(2)]?.let { return it }
            if (k.endsWith("s")) map[k.dropLast(1)]?.let { return it }
        } else if (k.startsWith("ال")) map[k.drop(2)]?.let { return it }
        return null
    }
}
