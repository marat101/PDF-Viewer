package ru.marat.viewplayground.pdf_reader.reader.layout.items

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt

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
        .drawBehind {
            if (bitmap != null || colors.alwaysShowBackground)
                drawRect(colors.backgroundColor)
        }
        .layoutId(page.index)



    Layout(
        modifier = pageModifier,
        content = {
            if (bitmap != null) PageImage(
                modifier = Modifier.fillMaxSize(),
                page = page,
                bitmap = bitmap
            )
            else Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(35.dp)
                        .align(Alignment.Center),
                    color = colors.progressIndicatorColor
                )
            }
        },
        measurePolicy = { measurables, constraints ->
            if (pageSize.isUnspecified) return@Layout layout(0, 0) {}
            val placeables = measurables.map {
                it.measure(
                    constraints.copy(
                        maxHeight = pageSize.height.roundToInt(),
                        maxWidth = pageSize.width.roundToInt()
                    )
                )
            }
            layout(pageSize.width.roundToInt(), pageSize.height.roundToInt()) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        })

    DisposableEffect(key1 = Unit) {
        println("PAGE ${page.index + 1}")
        page.onLoad()
        onDispose {
            println("PAGE ${page.index + 1} DISPOSE")
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
        contentDescription = "Page with scaled fragment"
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
                    scaledFragment.topLeft.x,
                    scaledFragment.topLeft.y
                ) {
                    drawImage(
                        image = scaledFragment.bitmap,
                        srcSize = scaledFragment.srcSize,
                        dstSize = scaledFragment.dstSize,
                        blendMode = BlendMode.Src
                    )
//                    drawRect(
//                        color = Color.Yellow.copy(0.2f),
// //                         style = Stroke((6).dp.toPx()),
//                        size = scaledFragment.dstSize.toSize()
//                    )
                }
            }
            canvas.restoreToCount(checkpoint)
        }
    }
}