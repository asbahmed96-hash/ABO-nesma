package com.abonesma.pdf
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.abonesma.core.ChapterDetector
import com.abonesma.data.BookE
import com.abonesma.data.Dao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
object ImportService {
    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { i -> val b = ByteArray(65536); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
    private fun displayName(ctx: Context, uri: Uri): String = try {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && i >= 0) c.getString(i) else null
        } ?: "Book"
    } catch (e: Exception) { "Book" }
    suspend fun import(ctx: Context, uri: Uri, dao: Dao, ar: Boolean, onStatus: (String) -> Unit): String = withContext(Dispatchers.IO) {
        fun t(e: String, a: String) = if (ar) a else e
        var file: File? = null
        try {
            file = PdfImporter.copyToAppStorage(ctx, uri)
            val hash = sha256(file)
            if (dao.bookByHash(hash) != null) { file.delete(); return@withContext t("This file was already imported", "هذا الملف مستورد من قبل") }
            val pages = PdfImporter.extractPages(ctx, file) { p, n -> onStatus(t("Reading page $p / $n", "قراءة الصفحة $p / $n")) }.toMutableList()
            var ocrFail = false; var ocrPages = 0
            val ocr by lazy { OcrEngine(ctx) }
            for (i in pages.indices) {
                if (pages[i].trim().length < 20 && !ocrFail) {
                    try { onStatus(t("OCR page ${i + 1} / ${pages.size}", "التعرف الضوئي للصفحة ${i + 1} / ${pages.size}")); pages[i] = ocr.ocrPdfPage(file, i); ocrPages++ }
                    catch (e: Throwable) { ocrFail = true }
                }
            }
            val text = pages.joinToString("\n")
            if (text.isBlank()) {
                file.delete()
                return@withContext if (ocrFail) t("Scanned PDF and OCR data (ara/eng traineddata) is not bundled in this build", "الملف ممسوح ضوئيًا وبيانات OCR (ara/eng) غير مضمنة في هذا الإصدار")
                else t("No readable text found", "لا يوجد نص قابل للقراءة")
            }
            val title = displayName(ctx, uri).removeSuffix(".pdf")
            val chapters = ChapterDetector.detect(text, title)
            dao.saveBook(BookE(title = title, path = file.absolutePath, hash = hash), chapters)
            t("Done: ${chapters.size} chapters", "تم: ${chapters.size} فصل") + (if (ocrPages > 0) t(" (OCR on $ocrPages pages)", " (OCR لـ $ocrPages صفحة)") else "") +
                (if (ocrFail) t(" - some blank pages skipped (OCR data missing)", " - تم تخطي صفحات فارغة (بيانات OCR غير متوفرة)") else "")
        } catch (e: Exception) {
            file?.delete()
            t("Cannot read this PDF: ${e.message}", "تعذرت قراءة الملف: ${e.message}")
        }
    }
}
