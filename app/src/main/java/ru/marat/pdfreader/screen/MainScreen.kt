package ru.marat.pdfreader.screen

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.layout.ReaderLayout
import ru.marat.pdf_reader.layout.state.LoadingState
import ru.marat.pdf_reader.layout.state.rememberReaderLayoutState
import androidx.core.net.toUri
import ru.marat.pdf_reader.layout.state.LayoutInfo
import ru.marat.pdf_reader.layout.state.toStringg

@Composable
fun MainScreen() {
    var uri by rememberSaveable {
        mutableStateOf<String?>(
            null
//        "content://com.android.providers.media.documents/document/document%3A1000000023"
        )
    }
    val context = LocalContext.current

    var ofs by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var orientation by rememberSaveable { mutableStateOf(true) }


    var visible by rememberSaveable { mutableStateOf(true) }
    var scrollDialogVisible by remember { mutableStateOf(false) }

    val padding by animateFloatAsState(targetValue = if (visible) 1f else 0.7f)
//    var spacing by remember { mutableFloatStateOf(8f) }
//    val height = remember { Animatable(0.1f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
//            .scale(0.65f)
    ) {
        AnimatedVisibility(
            modifier = Modifier.zIndex(1f),
            enter = expandVertically(),
            exit = shrinkVertically(),
            visible = visible
        ) {
            MainToolbar(
                ofs,
                zoom
            ) { newUri ->
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
        ) {
            if (uri != null) {
                val state = rememberReaderLayoutState(uri!!.toUri())
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ReaderLayout(
                        modifier = Modifier
                            .fillMaxSize()
//                        .fillMaxWidth(0.65f)
//                        .fillMaxHeight()
                            .drawBehind {
                                drawRect(
                                    color = Color.Red,
                                    style = Stroke(
                                        width = 4.dp.toPx()
                                    )
                                )
                            },
                        spacing = 6.dp,
                        layoutState = state,
                        onTap = { visible = !visible }
                    )
                    if (state.loadingState is LoadingState.Loading)
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = {
                                    (state.loadingState as LoadingState.Loading).progress
                                }
                            )
                            Text(
                                text = "Обработка…",
                                color = Color.White
                            )
                        }
                }
//                val layoutInfo by state.positionsState.layoutInfo.collectAsState()
//                LayoutInfo(layoutInfo)
                ScrollToPageDialog(
                    visible = scrollDialogVisible,
                    onDismiss = { scrollDialogVisible = false },
                    onScroll = {
                        val success = state.positionsState.scrollToPage(it)
                        if (success) scrollDialogVisible = false
                        success
                    }
                )
                LaunchedEffect(uri) {
                    launch {
                        state.positionsState.layoutInfo.collectLatest {
                            ofs = it.offset
                            zoom = it.zoom
                        }
                    }
                    launch {
                        snapshotFlow { orientation }.collectLatest {
                            state.positionsState.setOrientation(if (orientation) Orientation.Vertical else Orientation.Horizontal)
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
                orientation = if (orientation) Orientation.Vertical else Orientation.Horizontal,
                onOpenScrollDialog = {
                    scrollDialogVisible = true
                },
                onOrientationChange = {
                    orientation = it == Orientation.Vertical
                }
            )
        }
    }
}

@Composable
private fun BoxScope.LayoutInfo(layoutInfo: LayoutInfo) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.35f)
            .background(Color.White)
            .align(Alignment.CenterEnd)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Viewport size:\n${layoutInfo.viewportSize}"
        )
        Text(
            text = "Horizontal bounds:\n${layoutInfo.horizontalBounds}"
        )
        Text(
            text = "Vertical bounds:\n${layoutInfo.verticalBounds}"
        )
        runCatching {
            Text(
                text = "Layout position:\n${
                    layoutInfo.getLayoutPosition().toStringg()
                }\ncenter:\n${layoutInfo.getLayoutPosition().center.run { "x=$x\ny=$y" }}"
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Loaded pages:")
            layoutInfo.visiblePages.forEach {
                Text("index: ${it.index}\n${it.rect.toStringg()}")
                Spacer(modifier = Modifier.size(3.dp))
            }
        }
    }
}