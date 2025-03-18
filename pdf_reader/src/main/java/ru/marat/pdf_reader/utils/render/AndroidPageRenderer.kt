package ru.marat.pdf_reader.utils.render

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import ru.marat.viewplayground.pdf_reader.reader.layout.items.ScaledPage
import kotlin.math.roundToInt

class AndroidPageRenderer(
    private val mutex: Mutex,
    private val onCreateRenderer: () -> PdfRenderer
) : PageRenderer {

    private val matrix = Matrix()

    companion object {
        const val MAX_BITMAP_SIZE = 100 * 1024 * 1024 //100MB
    }

    override suspend fun renderPage(
        index: Int,
        pageSize: Size,
        onComplete: (ImageBitmap) -> Unit
    ) = coroutineScope {
        mutex.lock()
        closeIfNotActive{}
        val pdfRenderer = onCreateRenderer()
        val page = runCatching { pdfRenderer.openPage(index) }.getOrElse { throw CancellationException() }
        closeIfNotActive { page.close(); pdfRenderer.close() }
        val bm: Bitmap = createBitmap(pageSize.width, pageSize.height)
        closeIfNotActive { page.close(); pdfRenderer.close(); bm.recycle() }
        page.render(
            bm,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )
        page.close()
        pdfRenderer.close()
        mutex.unlock()
        withContext(Dispatchers.Main.immediate) { onComplete(bm.asImageBitmap()) }
    }

    override suspend fun renderPageFragment(
        index: Int,
        pageSize: Rect,
        scaledFragment: Rect,
        scale: Float,
        onComplete: (ScaledPage) -> Unit
    ) {
        val pdfRenderer = onCreateRenderer()
        val page = pdfRenderer.openPage(index)

        val scaledSize = scaledFragment.size * scale
        val bm: Bitmap = createBitmap(scaledSize.width, scaledSize.height)

        val sx = (pageSize.width / page.width) * scale
        val sy = (pageSize.height / page.height) * scale
        matrix.postScale(sx, sy)
        val dx = (pageSize.left - scaledFragment.left) * scale
        val dy = (pageSize.top - scaledFragment.top) * scale
        matrix.postTranslate(dx, dy)

        page.render(
            bm,
            null,
            matrix,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        page.close()
        pdfRenderer.close()
        matrix.reset()
        withContext(Dispatchers.Main.immediate) {
            onComplete(
                ScaledPage(
                    pageSize = pageSize,
                    scaledFragment = scaledFragment,
                    bitmap = bm.asImageBitmap()
                )
            )
        }
    }

    private suspend inline fun closeIfNotActive(crossinline beforeCancel: () -> Unit) {
        coroutineScope {
            if (!isActive) {
                beforeCancel()
                mutex.unlock()
                throw CancellationException()
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
}