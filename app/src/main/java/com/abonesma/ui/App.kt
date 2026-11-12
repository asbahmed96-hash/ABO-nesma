package com.abonesma.ui
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.abonesma.core.*
import com.abonesma.data.*
import com.abonesma.pdf.ImportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(dao: Dao, prefs: SharedPreferences, sharedUri: Uri?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ar by remember { mutableStateOf(prefs.getBoolean("ar", false)) }
    val t: T = { e, a -> if (ar) a else e }
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var tick by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val cur = stack.last()
    fun go(s: Screen) { stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun bump() { tick++ }
    fun doImport(uri: Uri) {
        if (busy) return
        scope.launch {
            busy = true; status = ""
            status = ImportService.import(ctx, uri, dao, ar) { status = it }
            busy = false; bump()
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) doImport(u) }
    LaunchedEffect(Unit) { sharedUri?.let { doImport(it) } }
    BackHandler(stack.size > 1) { back() }
    val title = when (cur) {
        Screen.Home -> "ABO NESMA 🦅"
        is Screen.Chapters -> cur.book.title
        is Screen.Lessons -> cur.ch.title
        is Screen.LessonView -> cur.l.title
        is Screen.Info -> t("Study tools", "أدوات المذاكرة")
        is Screen.Cards -> t("Flashcards", "البطاقات التعليمية")
        is Screen.Exam -> t("Exam", "الامتحان")
        is Screen.Result -> t("Result", "النتيجة")
        is Screen.Review -> t("Error Review", "مراجعة الأخطاء")
        Screen.Settings -> t("Settings", "الإعدادات")
    }
    CompositionLocalProvider(LocalLayoutDirection provides if (ar) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Scaffold(topBar = {
            TopAppBar(title = { Text(title, maxLines = 1) },
                navigationIcon = { if (stack.size > 1) TextButton({ back() }) { Text(if (ar) "→" else "←") } },
                actions = { if (cur == Screen.Home) TextButton({ go(Screen.Settings) }) { Text("⚙") } })
        }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (cur) {
                    Screen.Home -> HomeScreen(dao, t, tick, ::bump, ::go, status, busy) { pick.launch(arrayOf("application/pdf")) }
                    is Screen.Chapters -> ChaptersScreen(dao, t, tick, ::bump, ::go, cur.book)
                    is Screen.Lessons -> LessonsScreen(dao, t, tick, ::bump, ::go, cur.ch)
                    is Screen.LessonView -> LessonScreen(t, cur.l)
                    is Screen.Info -> InfoScreen(dao, t, ar, cur.ch)
                    is Screen.Cards -> CardsScreen(dao, t, cur.ch)
                    is Screen.Exam -> ExamScreen(t, cur.qs) { ans ->
                        scope.launch {
                            val g = ExamGrader.grade(cur.qs.map { it.correct }, ans)
                            dao.attempt(AttemptE(chapterId = cur.ch.id, score = g.score, total = g.total, at = System.currentTimeMillis()))
                            cur.qs.forEach { dao.deleteWrongByQ(cur.ch.id, it.text) }
                            g.wrongIdx.forEach { i -> val q = cur.qs[i]
                                dao.addWrong(WrongE(chapterId = cur.ch.id, q = q.text, options = q.options.joinToString("||"), chosen = ans[i] ?: -1, correct = q.correct, source = q.source, explain = q.explain)) }
                            stack.removeAt(stack.lastIndex); stack.add(Screen.Result(cur.ch, cur.qs, ans))
                        }
                    }
                    is Screen.Result -> ResultScreen(dao, t, cur, ::go)
                    is Screen.Review -> ReviewScreen(dao, t, tick, ::bump, ::go, cur.ch)
                    Screen.Settings -> SettingsScreen(t, ar) { ar = it; prefs.edit().putBoolean("ar", it).apply() }
                }
            }
        }
    }
}
