package ru.marat.pdfreader.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.layout.ReaderLayout
import ru.marat.pdf_reader.layout.state.LoadingState
import ru.marat.pdf_reader.layout.state.rememberReaderLayoutState

@Composable
fun MainScreen() {
    var uri by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var ofs by remember { mutableFloatStateOf(0f) }
    var visible by rememberSaveable { mutableStateOf(true) }
    var orientation by rememberSaveable { mutableStateOf(true) }
    val padding by animateFloatAsState(targetValue = if (visible) 1f else 0.7f)
//    var spacing by remember { mutableFloatStateOf(8f) }
//    val height = remember { Animatable(0.1f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
//            .scale(0.7f)
    ) {
        AnimatedVisibility(
            modifier = Modifier.zIndex(1f),
            enter = expandVertically(),
            exit = shrinkVertically(),
            visible = visible) {
            MainToolbar(ofs) { newUri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        newUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
                uri = newUri.toString().also {
                    println(it)
                }
            }
        }
//        Spacer(Modifier.fillMaxWidth().weight(height.value))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures {
                        visible = !visible
                    }
                }
        ) {
            if (uri != null) {
                val state = rememberReaderLayoutState(Uri.parse(uri))
                ReaderLayout(
                    modifier = Modifier
                        .fillMaxSize()
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
//                spacing = spacing.dp,
                    layoutState = state
                )
                LaunchedEffect(uri) {
                    launch {
                        snapshotFlow { state.positionsState.offsetY }.collectLatest {
                            ofs = it
                        }
                    }
                    launch {
                        snapshotFlow { orientation }.collectLatest {
                            state.positionsState.setOrientation(if(orientation) Orientation.Vertical else Orientation.Horizontal)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            BottomBar(
                orientation = if(orientation) Orientation.Vertical else Orientation.Horizontal,
            ) {
                orientation = it == Orientation.Vertical
            }
        }
    }
//    LaunchedEffect(uri) {
//        if (uri == null) return@LaunchedEffect
//        delay(4000)
//        spacing = 20f
//        height.animateTo(1f)
//    }
}