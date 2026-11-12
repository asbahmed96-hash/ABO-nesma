package com.abonesma.pdf
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
/** Offline OCR. REQUIRES app/src/main/assets/tessdata/ara.traineddata and eng.traineddata (not bundled yet). */
class OcrEngine(private val ctx: Context) {
    private fun dataDir(): File {
        val dir = File(ctx.filesDir, "tess/tessdata").also { it.mkdirs() }
        for (l in listOf("ara", "eng")) {
            val f = File(dir, "$l.traineddata")
            if (!f.exists()) ctx.assets.open("tessdata/$l.traineddata").use { i -> f.outputStream().use { i.copyTo(it) } }
        }
        return dir.parentFile!!
    }
    fun ocrPdfPage(pdf: File, pageIndex: Int): String {
        val api = TessBaseAPI(); check(api.init(dataDir().absolutePath, "ara+eng")) { "OCR data missing" }
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r -> r.openPage(pageIndex).use { pg ->
                val bmp = Bitmap.createBitmap(pg.width * 2, pg.height * 2, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
                pg.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                api.setImage(bmp); val t = api.utF8Text; api.recycle(); return t
            } }
        }
    }
}
