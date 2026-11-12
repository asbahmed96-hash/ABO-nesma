package com.abonesma.ui
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.abonesma.data.AppDb

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val dao = AppDb.get(this).dao()
        val prefs = getSharedPreferences("nesma", MODE_PRIVATE)
        val shared: Uri? = if (intent?.action == Intent.ACTION_SEND) intent.getParcelableExtra(Intent.EXTRA_STREAM) else null
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App(dao, prefs, shared) } } }
    }
}
