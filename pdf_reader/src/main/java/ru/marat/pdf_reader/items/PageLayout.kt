package ru.marat.viewplayground.pdf_reader.reader.layout.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import kotlin.math.roundToInt

internal val LocalPageColors = staticCompositionLocalOf { PageColors() }

@Composable
fun PageLayout(
    modifier: Modifier = Modifier,
    page: Page
) {
    val colors = LocalPageColors.current
    val pageModifier = modifier
        .fillMaxWidth()
        .drawBehind {
            if (page.bitmap != null || colors.alwaysShowBackground)
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
            if (page.bitmap != null) PageImage(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = Color.Yellow,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    },
                page = page
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
            val height = (constraints.maxWidth * page.ratio).roundToInt()
            val width = constraints.maxWidth
            val placeables = measurables.map { measurable ->
                measurable.measure(
                    constraints.copy(
                        minHeight = height
                    )
                )
            }
            layout(width, height) {
                placeables.forEach {
                    val x = (height / 2) - (it.height / 2)
                    val y = (width / 2) - (it.width / 2)
                    it.place(x, y)
                }
            }
        })

    DisposableEffect(key1 = Unit) {
        page.prepareBitmap()
        println("PAGE ${page.index + 1}")
        onDispose {
            println("PAGE ${page.index + 1} DISPOSE")
            page.onDispose()
        }
    }
}

data class PageColors(
    val backgroundColor: Color = Color.White,
    val progressIndicatorColor: Color = Color.Black,
    /** Если значение false фон будет прозрачным если страница не отрисована */
    val alwaysShowBackground: Boolean = true
)

@Composable
private fun PageImage(
    modifier: Modifier = Modifier,
    page: Page
) {
    val painter = remember(page.bitmap!!, page.scaledPage) {
        PageBitmapPainter(
            page.bitmap!!,
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
            val checkpoint = canvas.saveLayer(null, null)

            drawImage(
                image = page,
                dstSize = size.toIntSize(),
                blendMode = BlendMode.Src
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