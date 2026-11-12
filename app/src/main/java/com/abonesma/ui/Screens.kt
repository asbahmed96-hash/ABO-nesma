package com.abonesma.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.abonesma.core.*
import com.abonesma.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(dao: Dao, t: T, tick: Int, bump: () -> Unit, go: (Screen) -> Unit, status: String, busy: Boolean, onImport: () -> Unit) {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf(listOf<BookE>()) }
    var del by remember { mutableStateOf<BookE?>(null) }
    LaunchedEffect(tick) { books = dao.books() }
    Column(Modifier.padding(16.dp)) {
        Button(onClick = onImport, enabled = !busy) { Text(t("Import PDF", "استيراد PDF")) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        if (status.isNotBlank()) Text(status, Modifier.padding(vertical = 4.dp))
        if (books.isEmpty()) Text(t("No books yet. Import a PDF to start.", "لا توجد كتب بعد. استورد ملف PDF للبدء."), Modifier.padding(top = 16.dp))
        LazyColumn { items(books) { b ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { go(Screen.Chapters(b)) }) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(b.title, Modifier.weight(1f)); TextButton({ del = b }) { Text("🗑") }
                }
            }
        } }
    }
    del?.let { b -> ConfirmDialog(t("Delete this book and all its data?", "حذف الكتاب وكل بياناته؟"), t, { del = null }) {
        scope.launch { dao.deleteBookCascade(b.id); del = null; bump() }
    } }
}

@Composable
fun ChaptersScreen(dao: Dao, t: T, tick: Int, bump: () -> Unit, go: (Screen) -> Unit, book: BookE) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(listOf<ChapterE>()) }
    var rename by remember { mutableStateOf<ChapterE?>(null) }
    var del by remember { mutableStateOf<ChapterE?>(null) }
    var adding by remember { mutableStateOf(false) }
    LaunchedEffect(tick) { list = dao.chapters(book.id) }
    fun move(i: Int, d: Int) { val j = i + d; if (j !in list.indices) return
        scope.launch { dao.setChapterPos(list[i].id, list[j].pos); dao.setChapterPos(list[j].id, list[i].pos); bump() } }
    Column(Modifier.padding(12.dp)) {
        Button({ adding = true }) { Text(t("Add chapter", "إضافة فصل")) }
        LazyColumn { items(list.size) { i -> val c = list[i]
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(10.dp)) {
                Text(c.title, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { go(Screen.Lessons(c)) })
                Row { TextButton({ go(Screen.Lessons(c)) }) { Text(t("Lessons", "الدروس")) }
                    TextButton({ go(Screen.Info(c)) }) { Text(t("Study tools", "أدوات")) }
                    TextButton({ go(Screen.Cards(c)) }) { Text(t("Cards", "بطاقات")) } }
                Row { TextButton({ scope.launch {
                        val b = ContentBuilder.build(c.title, dao.lessons(c.id).map { Lesson(it.title, it.text) })
                        val qs = ContentBuilder.objective(b.bank, t("True", "صح"), t("False", "خطأ")); go(Screen.Exam(c, qs)) } }) { Text(t("Exam", "امتحان")) }
                    TextButton({ go(Screen.Review(c)) }) { Text(t("Errors", "الأخطاء")) }
                    TextButton({ move(i, -1) }) { Text("↑") }; TextButton({ move(i, 1) }) { Text("↓") }
                    TextButton({ rename = c }) { Text("✎") }; TextButton({ del = c }) { Text("🗑") } }
            } }
        } }
    }
    if (adding) TextDialog(t("New chapter", "فصل جديد"), listOf(t("Title", "العنوان")), listOf(""), t, { adding = false }) { v ->
        scope.launch { dao.chapter(ChapterE(bookId = book.id, title = v[0].ifBlank { "Chapter" }, pos = (list.maxOfOrNull { it.pos } ?: -1) + 1)); adding = false; bump() } }
    rename?.let { c -> TextDialog(t("Rename", "تعديل الاسم"), listOf(t("Title", "العنوان")), listOf(c.title), t, { rename = null }) { v ->
        scope.launch { dao.renameChapter(c.id, v[0]); rename = null; bump() } } }
    del?.let { c -> ConfirmDialog(t("Delete this chapter?", "حذف الفصل؟"), t, { del = null }) { scope.launch { dao.deleteChapterCascade(c.id); del = null; bump() } } }
}

@Composable
fun LessonsScreen(dao: Dao, t: T, tick: Int, bump: () -> Unit, go: (Screen) -> Unit, ch: ChapterE) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(listOf<LessonE>()) }
    var rename by remember { mutableStateOf<LessonE?>(null) }
    var del by remember { mutableStateOf<LessonE?>(null) }
    var adding by remember { mutableStateOf(false) }
    LaunchedEffect(tick) { list = dao.lessons(ch.id) }
    fun move(i: Int, d: Int) { val j = i + d; if (j !in list.indices) return
        scope.launch { dao.setLessonPos(list[i].id, list[j].pos); dao.setLessonPos(list[j].id, list[i].pos); bump() } }
    Column(Modifier.padding(12.dp)) {
        Button({ adding = true }) { Text(t("Add lesson", "إضافة درس")) }
        LazyColumn { items(list.size) { i -> val l = list[i]
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { go(Screen.LessonView(l)) }) { Column(Modifier.padding(10.dp)) {
                Text(l.title, fontWeight = FontWeight.Bold)
                Row { TextButton({ move(i, -1) }) { Text("↑") }; TextButton({ move(i, 1) }) { Text("↓") }
                    TextButton({ rename = l }) { Text("✎") }; TextButton({ del = l }) { Text("🗑") } }
            } }
        } }
    }
    if (adding) TextDialog(t("New lesson", "درس جديد"), listOf(t("Title", "العنوان"), t("Text", "النص")), listOf("", ""), t, { adding = false }) { v ->
        scope.launch { dao.lesson(LessonE(chapterId = ch.id, title = v[0].ifBlank { "Lesson" }, text = v[1], pos = (list.maxOfOrNull { it.pos } ?: -1) + 1)); adding = false; bump() } }
    rename?.let { l -> TextDialog(t("Rename", "تعديل الاسم"), listOf(t("Title", "العنوان")), listOf(l.title), t, { rename = null }) { v ->
        scope.launch { dao.renameLesson(l.id, v[0]); rename = null; bump() } } }
    del?.let { l -> ConfirmDialog(t("Delete this lesson?", "حذف الدرس؟"), t, { del = null }) { scope.launch { dao.deleteLesson(l.id); del = null; bump() } } }
}

@Composable
fun LessonScreen(t: T, l: LessonE) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var tab by remember { mutableStateOf(0) }
    val tr = remember { runCatching { com.abonesma.data.DictLoader.load(ctx) }.getOrNull() }
    val tabs = listOf(t("Text", "النص"), t("Summary", "الملخص"), t("Terms", "المصطلحات"), t("Translate", "ترجمة"))
    Column {
        TabRow(tab) { tabs.forEachIndexed { i, s -> Tab(tab == i, { tab = i }, text = { Text(s, fontSize = 12.sp) }) } }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (tab) {
                0 -> Text(l.text.ifBlank { t("(empty)", "(فارغ)") })
                1 -> Summarizer.summarize(l.text).let { s -> if (s.isEmpty()) Text(t("Not enough text to summarize", "النص قصير للتلخيص")) else s.forEach { Text("• $it", Modifier.padding(vertical = 4.dp)) } }
                2 -> DefinitionExtractor.extract(l.text).let { d -> if (d.isEmpty()) Text(t("No definitions found in this lesson", "لا توجد تعريفات في هذا الدرس")) else d.forEach { Text("${it.term}: ${it.meaning}", Modifier.padding(vertical = 4.dp)) } }
                else -> {
                    Text(t("Dictionary-based translation: limited, not professional.", "ترجمة قاموسية محدودة وليست احترافية."), fontSize = 12.sp)
                    val out = tr?.auto(l.text) ?: t("Dictionary unavailable", "القاموس غير متاح")
                    Text(l.text.take(2000), Modifier.padding(vertical = 8.dp)); HorizontalDivider()
                    Text(out.take(2000), Modifier.padding(vertical = 8.dp))
                    Button({ clip.setText(AnnotatedString(out)) }) { Text(t("Copy translation", "نسخ الترجمة")) }
                }
            }
        }
    }
}

@Composable
fun InfoScreen(dao: Dao, t: T, ar: Boolean, ch: ChapterE) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf<ChapterContent?>(null) }
    var tab by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf("") }
    val tr = remember { runCatching { com.abonesma.data.DictLoader.load(ctx) }.getOrNull() }
    LaunchedEffect(ch.id) { content = withContext(Dispatchers.Default) { ContentBuilder.build(ch.title, dao.lessons(ch.id).map { Lesson(it.title, it.text) }) } }
    val saveCb: (android.net.Uri?) -> Unit = { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(pending.toByteArray(Charsets.UTF_8)) } }
                .onSuccess { msg = t("Saved", "تم الحفظ") }.onFailure { msg = t("Save failed", "فشل الحفظ") }
        }
    }
    val txtL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain"), saveCb)
    val jsonL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"), saveCb)
    val c = content
    if (c == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val tabs = listOf(t("Summary", "ملخص"), t("Formulas", "قوانين"), t("Definitions", "تعريفات"), t("Compare", "مقارنات"), t("Questions", "أسئلة"))
    Column {
        Row(Modifier.padding(horizontal = 8.dp)) {
            TextButton({ pending = ChapterExporter.txt(c); txtL.launch("${ch.title}.txt") }) { Text("TXT") }
            TextButton({ pending = ChapterExporter.json(c); jsonL.launch("${ch.title}.json") }) { Text("JSON") }
            if (msg.isNotBlank()) Text(msg, Modifier.align(Alignment.CenterVertically))
        }
        ScrollableTabRow(tab) { tabs.forEachIndexed { i, s -> Tab(tab == i, { tab = i }, text = { Text(s, fontSize = 12.sp) }) } }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            fun empty() = t("Nothing reliable was found in the source.", "لم يتم العثور على محتوى موثوق في المصدر.")
            when (tab) {
                0 -> c.summaries.forEach { (title, s) -> Text(title, fontWeight = FontWeight.Bold); s.forEach { Text("• $it") }; Spacer(Modifier.height(8.dp)) }
                1 -> if (c.formulas.isEmpty()) Text(empty()) else c.formulas.forEach { Text(it.expr, fontWeight = FontWeight.Bold); Text(t("Lesson: ", "الدرس: ") + it.source, fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
                2 -> if (c.defs.isEmpty()) Text(empty()) else c.defs.forEach {
                    Text(it.term, fontWeight = FontWeight.Bold); Text(it.meaning)
                    tr?.let { x -> Text(x.auto(it.term + ": " + it.meaning), fontSize = 12.sp) }
                    Text(t("Lesson: ", "الدرس: ") + it.lesson, fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
                3 -> if (c.comps.isEmpty()) Text(empty()) else c.comps.forEach {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(it.a, Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); Text(it.b, Modifier.weight(1f)) }; HorizontalDivider() }
                else -> {
                    c.bank.notes.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error) }
                    c.bank.mcq.forEachIndexed { i, q -> Text("${i + 1}. ${q.q}", fontWeight = FontWeight.Bold)
                        q.options.forEachIndexed { j, o -> Text("${'A' + j}) $o" + if (j == q.correct) " ✓" else "") }; Spacer(Modifier.height(8.dp)) }
                    c.bank.tf.forEach { Text("${it.statement} → ${if (it.answer) t("True", "صح") else t("False", "خطأ")}", Modifier.padding(vertical = 4.dp)) }
                    c.bank.essay.forEach { Text(it.q, fontWeight = FontWeight.Bold); Text(t("Reference: ", "المرجع: ") + it.reference, Modifier.padding(bottom = 8.dp)) }
                }
            }
        }
    }
}

@Composable
fun CardsScreen(dao: Dao, t: T, ch: ChapterE) {
    val scope = rememberCoroutineScope()
    var cards by remember { mutableStateOf<List<Flashcard>?>(null) }
    var i by remember { mutableStateOf(0) }
    var flip by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<CardStateE?>(null) }
    LaunchedEffect(ch.id) { cards = withContext(Dispatchers.Default) { ContentBuilder.build(ch.title, dao.lessons(ch.id).map { Lesson(it.title, it.text) }).cards } }
    val list = cards
    if (list == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    if (list.isEmpty()) { Text(t("No cards could be made from this chapter.", "تعذر إنشاء بطاقات من هذا الفصل."), Modifier.padding(16.dp)); return }
    val card = list[i]; val key = "${ch.id}:${card.front.hashCode()}"
    LaunchedEffect(key) { state = dao.card(key) }
    fun save(m: Int, r: Boolean) { val s = CardStateE(key, m, r); state = s; scope.launch { dao.putCard(s) } }
    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${i + 1} / ${list.size}")
        Card(Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp).clickable { flip = !flip }) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) { Text(if (flip) card.back else card.front, fontSize = 20.sp) }
        }
        Text(t("Tap card to flip", "اضغط على البطاقة لقلبها"), fontSize = 12.sp)
        Text(t("Mastery: ", "الإتقان: ") + (state?.mastery ?: 0) + "/3" + if (state?.review == true) "  ★" else "")
        Row { listOf(1, 2, 3).forEach { m -> TextButton({ save(m, state?.review ?: false) }) { Text(listOf(t("Hard", "صعب"), t("OK", "متوسط"), t("Easy", "سهل"))[m - 1]) } } }
        TextButton({ save(state?.mastery ?: 0, !(state?.review ?: false)) }) { Text(t("Mark for review ★", "تحديد للمراجعة ★")) }
        Row { Button({ if (i > 0) { i--; flip = false } }) { Text(t("Prev", "السابق")) }; Spacer(Modifier.width(12.dp))
            Button({ if (i < list.lastIndex) { i++; flip = false } }) { Text(t("Next", "التالي")) } }
    }
}

@Composable
fun ExamScreen(t: T, qs: List<Q>, onFinish: (Map<Int, Int>) -> Unit) {
    if (qs.isEmpty()) { Text(t("Not enough verifiable questions in this chapter.", "لا توجد أسئلة موثوقة كافية في هذا الفصل."), Modifier.padding(16.dp)); return }
    var idx by remember { mutableStateOf(0) }
    val ans = remember { mutableStateMapOf<Int, Int>() }
    var left by remember { mutableStateOf(qs.size * 60) }
    var done by remember { mutableStateOf(false) }
    fun finish() { if (!done) { done = true; onFinish(ans.toMap()) } }
    LaunchedEffect(Unit) { while (left > 0 && !done) { delay(1000); left-- }; if (left <= 0) finish() }
    val q = qs[idx]
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("%02d:%02d".format(left / 60, left % 60) + "   ${idx + 1} / ${qs.size}")
        Text(q.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
        q.options.forEachIndexed { j, o -> Row(Modifier.fillMaxWidth().clickable { ans[idx] = j }, verticalAlignment = Alignment.CenterVertically) {
            RadioButton(ans[idx] == j, { ans[idx] = j }); Text("${'A' + j}) $o") } }
        Row(Modifier.padding(top = 12.dp)) {
            Button({ if (idx > 0) idx-- }, enabled = idx > 0) { Text(t("Prev", "السابق")) }; Spacer(Modifier.width(8.dp))
            Button({ if (idx < qs.lastIndex) idx++ }, enabled = idx < qs.lastIndex) { Text(t("Next", "التالي")) }; Spacer(Modifier.width(8.dp))
            Button({ finish() }) { Text(t("Finish", "إنهاء")) } }
    }
}

@Composable
fun ResultScreen(dao: Dao, t: T, r: Screen.Result, go: (Screen) -> Unit) {
    val g = remember { ExamGrader.grade(r.qs.map { it.correct }, r.ans) }
    var hist by remember { mutableStateOf(listOf<AttemptE>()) }
    LaunchedEffect(Unit) { hist = dao.attempts(r.ch.id) }
    LazyColumn(Modifier.padding(16.dp)) {
        item {
            Text("${g.score} / ${g.total}  (${"%.1f".format(g.percent)}%)", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Row { Button({ go(Screen.Exam(r.ch, r.qs)) }) { Text(t("Retake", "إعادة الامتحان")) }; Spacer(Modifier.width(8.dp))
                Button({ go(Screen.Review(r.ch)) }) { Text(t("Review errors", "مراجعة الأخطاء")) } }
            Spacer(Modifier.height(12.dp))
        }
        items(r.qs.size) { i -> val q = r.qs[i]; val ok = i !in g.wrongIdx
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(10.dp)) {
                Text((if (ok) "✅ " else "❌ ") + q.text, fontWeight = FontWeight.Bold)
                if (!ok) { Text(t("Your answer: ", "إجابتك: ") + (r.ans[i]?.let { q.options[it] } ?: t("none", "بدون"))) }
                Text(t("Correct: ", "الصحيح: ") + q.options[q.correct]); if (q.explain.isNotBlank()) Text(q.explain, fontSize = 12.sp)
            } } }
        item { Spacer(Modifier.height(8.dp)); Text(t("Attempt history", "سجل المحاولات"), fontWeight = FontWeight.Bold) }
        items(hist) { a -> Text("${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(a.at))}  ${a.score}/${a.total}") }
    }
}

@Composable
fun ReviewScreen(dao: Dao, t: T, tick: Int, bump: () -> Unit, go: (Screen) -> Unit, ch: ChapterE) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(listOf<WrongE>()) }
    LaunchedEffect(tick) { list = dao.wrongs(ch.id) }
    if (list.isEmpty()) { Text(t("No mistakes recorded. Great!", "لا توجد أخطاء مسجلة."), Modifier.padding(16.dp)); return }
    Column(Modifier.padding(12.dp)) {
        Button({ go(Screen.Exam(ch, list.map { w -> Q(w.q, w.options.split("||"), w.correct, w.source, w.explain) })) }) { Text(t("Retest these questions", "اختبر نفسك في هذه الأسئلة")) }
        LazyColumn { items(list) { w -> val opts = w.options.split("||")
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(10.dp)) {
                Text(w.q, fontWeight = FontWeight.Bold)
                Text(t("Your answer: ", "إجابتك: ") + (opts.getOrNull(w.chosen) ?: t("none", "بدون")))
                Text(t("Correct: ", "الصحيح: ") + opts[w.correct]); if (w.explain.isNotBlank()) Text(w.explain, fontSize = 12.sp)
                Text(t("Lesson/Source: ", "الدرس/المصدر: ") + w.source + " — " + ch.title, fontSize = 12.sp)
                TextButton({ scope.launch { dao.deleteWrong(w.id); bump() } }) { Text(t("Remove", "حذف")) }
            } } } }
    }
}

@Composable
fun SettingsScreen(t: T, ar: Boolean, setAr: (Boolean) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(t("Arabic interface (RTL)", "الواجهة العربية (RTL)"), Modifier.weight(1f)); Switch(ar, setAr) }
        Spacer(Modifier.height(16.dp))
        Text(t("Everything runs locally on your phone. No internet, no accounts, no cloud.", "كل شيء يعمل محليًا على هاتفك. بلا إنترنت أو حسابات أو سحابة."))
        Spacer(Modifier.height(8.dp))
        Text(t("Translation is dictionary-based and limited; it is not professional translation.", "الترجمة قاموسية ومحدودة وليست ترجمة احترافية."), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp)); Text("ABO NESMA 🦅 v0.1.0", fontSize = 12.sp)
    }
}
