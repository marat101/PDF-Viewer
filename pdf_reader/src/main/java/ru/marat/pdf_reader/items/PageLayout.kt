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
import kotlinx.coroutines.Dispatchers

internal val LocalPageColors = staticCompositionLocalOf { PageColors() }

@Composable
fun PageLayout(
    modifier: Modifier = Modifier,
    page: Page
) {
    val configuration = LocalConfiguration.current
    val colors = LocalPageColors.current
    val bitmap by page.bitmap.collectAsState(initial = null, Dispatchers.Main)
    val pageSize by page.size.collectAsState()
    val pageModifier = modifier
        .drawBehind {
            if (bitmap != null || colors.alwaysShowBackground)
                drawRect(colors.backgroundColor)
        }
        .drawWithContent {
            drawContent()
            drawRect(
                color = Color.Green,
                style = Stroke(width = 4.dp.toPx())
            )
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
//            val screenSize = DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp).toSize()
//            placeByLayoutOrientation(page, constraints, measurables)
            if (pageSize.isUnspecified) {
                return@Layout layout(0, 0) {}
            }
            val placeables = measurables.map {
                it.measure(constraints.copy(
                    maxHeight = pageSize.height.toInt(),
                    maxWidth = pageSize.width.toInt()
                ))
            }
            layout(pageSize.width.toInt(), pageSize.height.toInt()) {
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
    val painter = remember(bitmap!!, page.scaledPage) {
        PageBitmapPainter(
            bitmap!!,
            page.scaledPage
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
            drawRect(
                color = Color.Yellow,
                style = Stroke(width = 4.dp.toPx())
            )
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
                }
            }
            canvas.restoreToCount(checkpoint)
        }
    }
}