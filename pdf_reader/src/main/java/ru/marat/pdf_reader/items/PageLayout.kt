package ru.marat.viewplayground.pdf_reader.reader.layout.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import ru.marat.pdf_reader.utils.toIntSize

val LocalPageColors = staticCompositionLocalOf { PageColors() }

@Composable
fun PageLayout(
    modifier: Modifier = Modifier,
    page: Page
) {
    val colors = LocalPageColors.current
    val bitmap by page.bitmap.collectAsState(initial = null, Dispatchers.Main)
    val pageSize by page.size.collectAsState()
    val pageModifier = modifier
        .background(
            if (bitmap != null || colors.alwaysShowBackground) colors.backgroundColor
            else Color.Transparent
        )
        .layoutId(page.index)



    Layout(
        modifier = pageModifier,
        content = {
            if (bitmap != null) PageImage(
                modifier = Modifier.fillMaxSize(),
                page = page,
                bitmap = bitmap
            ) else Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(35.dp)
                        .align(Alignment.Center),
                    color = colors.progressIndicatorColor
                )
            }
        },
        measurePolicy = { measurables, constraints ->
            if (pageSize == IntSize.Zero) return@Layout layout(0, 0) {}
            val placeables = measurables.map {
                it.measure(
                    constraints.copy(
                        maxHeight = pageSize.height,
                        maxWidth = pageSize.width
                    )
                )
            }
            layout(pageSize.width, pageSize.height) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        })

    DisposableEffect(key1 = Unit) {
        page.onLoad()
        onDispose {
            page.onDispose()
        }
    }
}

data class PageColors(
    val backgroundColor: Color = Color.White,
    val progressIndicatorColor: Color = Color.Black,
    /** Если значение false фон будет прозрачным когда страница не отрисована */
    val alwaysShowBackground: Boolean = true
)

@Composable
private fun PageImage(
    modifier: Modifier = Modifier,
    page: Page,
    bitmap: ImageBitmap?
) {
    val scaledFragment by page.scaledPage.collectAsState()
    val painter = remember(bitmap!!, scaledFragment) {
        PageBitmapPainter(
            bitmap,
            scaledFragment
        )
    }

    Image(
        modifier = modifier,
        painter = painter,
        contentDescription = null
    )
}

private class PageBitmapPainter(
    private val page: ImageBitmap,
    private val scaledFragment: ScaledPage?
) : Painter() {

    override val intrinsicSize: Size
        get() = page.size()

    override fun DrawScope.onDraw() {
        drawContext.canvas.nativeCanvas.let { canvas ->
            val checkpoint = canvas.saveLayer(null, null)

            drawImage(
                image = page,
                srcSize = page.size().toIntSize(),
                dstSize = size.toIntSize(),
                blendMode = BlendMode.Src,
            )

            if (scaledFragment != null) {
                translate(
                    scaledFragment.topLeft.x.toFloat(),
                    scaledFragment.topLeft.y.toFloat()
                ) {
                    drawImage(
                        image = scaledFragment.bitmap,
                        dstSize = scaledFragment.dstSize,
                        blendMode = BlendMode.Src,
//                        dstOffset = IntOffset(scaledFragment.rect.topLeft.x.toInt(),scaledFragment.rect.topLeft.y.toInt()),
                    )
                }
            }
            canvas.restoreToCount(checkpoint)
        }
    }
}