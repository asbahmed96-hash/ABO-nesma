package com.abonesma.data
import android.content.Context
import com.abonesma.core.Translator
object DictLoader {
    fun load(ctx: Context): Translator = Translator.parse(ctx.assets.open("dict_en_ar.tsv").bufferedReader(Charsets.UTF_8).use { it.readText() })
}
