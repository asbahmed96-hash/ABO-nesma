package com.abonesma.core
import org.junit.Assert.*
import org.junit.Test
class CoreTest {
    val sample = """
Chapter 1 Motion
Lesson 1 Speed
Speed is defined as the distance travelled per unit of time in a motion.
v = d / t
Velocity refers to speed in a given direction with magnitude included.
Acceleration means the rate of change of velocity over a period of time.
Force: a push or pull acting upon an object that changes its motion.
الفصل الثاني الطاقة
الدرس الأول الشغل
الشغل هو القوة المبذولة مضروبة في الإزاحة في اتجاه القوة نفسها.
"""
    @Test fun chapters() { val c = ChapterDetector.detect(sample); assertEquals(2, c.size) }
    @Test fun defs() { assertTrue(DefinitionExtractor.extract(sample).size >= 3) }
    @Test fun formula() { assertEquals("v = d / t", FormulaExtractor.extract(sample).first().expr) }
    @Test fun questions() {
        val q = QuestionGenerator.generate(DefinitionExtractor.extract(sample))
        assertTrue(q.mcq.all { it.options.size == 4 && it.options[it.correct] == it.source })
    }
    @Test fun grade() { assertEquals(50.0, ExamGrader.grade(listOf(0, 1), mapOf(0 to 0, 1 to 2)).percent, 0.01) }
}
class MoreTests {
    @Test fun translator() {
        val tr = Translator(listOf("force" to "القوة", "rate of change" to "معدل التغير", "velocity" to "السرعة المتجهة"))
        assertEquals("القوة and معدل التغير", tr.translate("Force and rate of change", true))
        assertEquals("force", tr.translate("القوة", false))
        assertTrue(Translator.isArabic("الشغل هو القوة"))
    }
    @Test fun summarizerKeepsOriginalSentences() {
        val t = "Energy is the capacity to do work in physics. Energy comes in many forms such as heat. Work transfers energy between objects. The weather was nice today in the city. Energy cannot be created or destroyed ever."
        val s = Summarizer.summarize(t, 2); assertEquals(2, s.size); assertTrue(s.all { t.contains(it) })
    }
    @Test fun comparison() {
        val c = ComparisonExtractor.extract("Speed has no direction in physics, whereas velocity includes direction and magnitude.")
        assertEquals(1, c.size)
    }
    @Test fun thinSourceReportsShortfall() {
        val q = QuestionGenerator.generate(listOf(Definition("A", "first long meaning here"), Definition("B", "second long meaning here")))
        assertTrue(q.mcq.isEmpty()); assertTrue(q.notes.isNotEmpty())
    }
    @Test fun contentBuilder() {
        val c = ContentBuilder.build("Ch", listOf(Lesson("L1", "Speed is defined as the distance travelled per unit of time.\nv = d / t")))
        assertEquals(1, c.formulas.size); assertEquals(1, c.defs.size)
    }
}
