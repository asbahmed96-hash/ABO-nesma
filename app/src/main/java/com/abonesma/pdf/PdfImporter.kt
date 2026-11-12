package com.abonesma.pdf
import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
object PdfImporter {
    fun copyToAppStorage(ctx: Context, uri: Uri): File {
        val out = File(ctx.filesDir, "books/${System.currentTimeMillis()}.pdf").also { it.parentFile?.mkdirs() }
        ctx.contentResolver.openInputStream(uri)!!.use { i -> out.outputStream().use { i.copyTo(it) } }
        return out
    }
    /** Returns per-page text; pages with no text need OCR (see OcrEngine). */
    fun extractPages(ctx: Context, file: File, onProgress: (Int, Int) -> Unit): List<String> {
        PDFBoxResourceLoader.init(ctx)
        PDDocument.load(file).use { doc ->
            val n = doc.numberOfPages; val st = PDFTextStripper()
            return (1..n).map { p -> st.startPage = p; st.endPage = p; onProgress(p, n); st.getText(doc) }
        }
    }
}
