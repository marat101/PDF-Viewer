package ru.marat.pdfreader.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.marat.pdf_reader.layout.ReaderLayout
import ru.marat.pdf_reader.layout.state.LoadingState
import ru.marat.pdf_reader.layout.state.rememberReaderLayoutState

@Composable
fun MainScreen() {
    var uri by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var ofs by remember { mutableFloatStateOf(0f) }
//    val height = remember { Animatable(0.1f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
//            .scale(0.7f)
    ) {
        MainToolbar(ofs) { newUri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    newUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }catch (e: SecurityException){
                e.printStackTrace()
            }
            uri = newUri.toString().also {
                println(it)
            }
        }
//        Spacer(Modifier.fillMaxWidth().weight(height.value))
        if (uri != null) {
            val state = rememberReaderLayoutState(Uri.parse(uri))
            ReaderLayout(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        if (state.loadingState is LoadingState.Loading)
                            drawArc(
                                color = Color.Cyan,
                                startAngle = -90f,
                                sweepAngle = 360f * (state.loadingState as LoadingState.Loading).progress,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx())
                            )
                    },
                state = state
            ) {
                ofs = it
            }
        }
    }
//    LaunchedEffect(uri) {
//        if (uri == null) return@LaunchedEffect
//        delay(4000)
//        height.animateTo(1f)
//    }
}