package ru.marat.pdf_reader.items.render

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import ru.marat.pdf_reader.items.render.AndroidPageRenderer.Companion.MAX_BITMAP_SIZE
import ru.marat.pdf_reader.utils.pdf_info.RendererScope
import ru.marat.pdf_reader.utils.toIntSize
import ru.marat.viewplayground.pdf_reader.reader.layout.items.ScaledPage
import kotlin.math.roundToInt

class AndroidPageRenderer(
    private val rendererScope: RendererScope
) : PageRenderer {

    private val matrix = Matrix()

    companion object {
        const val MAX_BITMAP_SIZE = 100 * 1024 * 1024 //100MB
    }

    override suspend fun renderPage(
        index: Int,
        pageSize: IntSize,
    ) = coroutineScope {
        closeIfNotActive {}
        rendererScope.use { pdfRenderer ->
            val page =
                runCatching { pdfRenderer.openPage(index) }.getOrElse { throw CancellationException() }
            closeIfNotActive { page.close() }
            val bm: Bitmap = createBitmap(pageSize.width.toFloat(), pageSize.height.toFloat())
            closeIfNotActive { page.close(); bm.recycle() }
            page.render(
                bm,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )
            page.close()
            bm.asImageBitmap()
        }
    }

    override suspend fun renderPageFragment(
        index: Int,
        pageSize: IntRect,
        scaledFragment: IntRect,
        scale: Float,
    ): ScaledPage {
        return rendererScope.use { pdfRenderer ->
            val page = runCatching { pdfRenderer.openPage(index) }.getOrElse { throw CancellationException() }
            closeIfNotActive { page.close() }

            val scaledSize = (scaledFragment.size.toSize() * scale).toIntSize()
            val bm: Bitmap = createBitmap(scaledSize.width.toFloat(), scaledSize.height.toFloat())

            val sx = (pageSize.width / page.width.toFloat()) * scale
            val sy = (pageSize.height / page.height.toFloat()) * scale
            matrix.postScale(sx, sy)
            val dx = (pageSize.left - scaledFragment.left.toFloat()) * scale
            val dy = (pageSize.top - scaledFragment.top.toFloat()) * scale
            matrix.postTranslate(dx, dy)

            closeIfNotActive { page.close();bm.recycle(); matrix.reset() }
            page.render(
                bm,
                null,
                matrix,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            page.close()
            matrix.reset()
            ScaledPage(
                pageSize = pageSize,
                scaledFragment = scaledFragment,
                bitmap = bm.asImageBitmap()
            )
        }
    }
}

private suspend inline fun closeIfNotActive(crossinline beforeCancel: () -> Unit) {
    coroutineScope {
        if (!isActive) {
            beforeCancel()
            ensureActive()
        }
    }
}

private fun createBitmap(width: Float, height: Float): Bitmap {
    var intSize = IntSize(width.roundToInt(), height.roundToInt())
    val sizeInBytes = intSize.width * intSize.height * 4
    if (sizeInBytes > MAX_BITMAP_SIZE) {
        val scale = MAX_BITMAP_SIZE / sizeInBytes.toDouble()
        intSize = IntSize((intSize.width * scale).toInt(), (intSize.height * scale).toInt())
    }
    return Bitmap.createBitmap(intSize.width, intSize.height, Bitmap.Config.ARGB_8888)
}
