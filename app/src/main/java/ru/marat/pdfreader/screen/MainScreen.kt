package ru.marat.pdfreader.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ru.marat.pdf_reader.layout.ReaderLayout
import ru.marat.pdf_reader.layout.state.rememberReaderLayoutState
import ru.marat.pdf_reader.utils.pdf_info.AndroidPdfInfoProvider

@Composable
fun MainScreen() {
    var uri by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MainToolbar() { newUri ->
            context.contentResolver.takePersistableUriPermission(
                newUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            uri = newUri.toString().also {
                println(it)
            }
        }
        if (uri != null) {
            val state = rememberReaderLayoutState(AndroidPdfInfoProvider(context, Uri.parse(uri!!)))
            ReaderLayout(
                modifier = Modifier.weight(1f),
                state = state
            )
        }
    }
    LaunchedEffect(Unit) {
        val fastUri = sber_pdf
//            "content://com.android.providers.media.documents/document/document%3A1000000037"
        val hasPermission = context.contentResolver.persistedUriPermissions.any {
            it.uri.toString() == fastUri && it.isReadPermission
        }
        if (hasPermission) uri = fastUri
    }
}

private const val sber_pdf = "content://com.android.providers.downloads.documents/document/83"