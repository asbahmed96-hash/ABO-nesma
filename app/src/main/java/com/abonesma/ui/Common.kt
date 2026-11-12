package com.abonesma.ui
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abonesma.data.*

typealias T = (String, String) -> String

sealed interface Screen {
    data object Home : Screen
    data class Chapters(val book: BookE) : Screen
    data class Lessons(val ch: ChapterE) : Screen
    data class LessonView(val l: LessonE) : Screen
    data class Info(val ch: ChapterE) : Screen
    data class Cards(val ch: ChapterE) : Screen
    data class Exam(val ch: ChapterE, val qs: List<com.abonesma.core.Q>) : Screen
    data class Result(val ch: ChapterE, val qs: List<com.abonesma.core.Q>, val ans: Map<Int, Int>) : Screen
    data class Review(val ch: ChapterE) : Screen
    data object Settings : Screen
}

@Composable
fun TextDialog(title: String, labels: List<String>, initial: List<String>, t: T, onDismiss: () -> Unit, onOk: (List<String>) -> Unit) {
    val vals = remember { mutableStateListOf(*initial.toTypedArray()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column { labels.forEachIndexed { i, l -> OutlinedTextField(vals[i], { vals[i] = it }, label = { Text(l) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) } } },
        confirmButton = { TextButton({ onOk(vals.toList()) }) { Text(t("OK", "موافق")) } },
        dismissButton = { TextButton(onDismiss) { Text(t("Cancel", "إلغاء")) } })
}

@Composable
fun ConfirmDialog(msg: String, t: T, onDismiss: () -> Unit, onOk: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, text = { Text(msg) },
        confirmButton = { TextButton(onOk) { Text(t("Yes", "نعم")) } },
        dismissButton = { TextButton(onDismiss) { Text(t("No", "لا")) } })
}
