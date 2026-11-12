package com.abonesma.core
import org.json.JSONArray
import org.json.JSONObject
object Exporter {
    fun definitionsTxt(d: List<Definition>) = d.joinToString("\n\n") { "${it.term}\n${it.meaning}" }
    fun definitionsJson(d: List<Definition>): String = JSONArray(d.map { JSONObject().put("term", it.term).put("meaning", it.meaning) }).toString(2)
    fun mcqJson(q: List<Mcq>): String = JSONArray(q.map { JSONObject().put("q", it.q).put("options", JSONArray(it.options)).put("correct", it.correct).put("source", it.source) }).toString(2)
    fun mcqTxt(q: List<Mcq>) = q.mapIndexed { i, m -> "${i + 1}. ${m.q}\n" + m.options.mapIndexed { j, o -> "  ${'A' + j}) $o" }.joinToString("\n") + "\nAnswer: ${'A' + m.correct}" }.joinToString("\n\n")
}
