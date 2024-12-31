package ru.marat.pdf_reader.utils.render

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.marat.viewplayground.pdf_reader.reader.layout.items.ScaledPage
import kotlin.math.roundToInt

class AndroidPageRenderer(
    private val pdfRenderer: PdfRenderer
) : PageRenderer {

    private val mutex = Mutex()
    private val matrix = Matrix()

    override suspend fun renderPage(
        index: Int,
        pageSize: Rect,
        onComplete: (ImageBitmap) -> Unit
    ) = mutex.withLock {
        val page = pdfRenderer.openPage(index)
        val bm: Bitmap = createBitmap(pageSize.width, pageSize.height)

        page.render(
            bm,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )
        page.close()
        withContext(Dispatchers.Main.immediate) { onComplete(bm.asImageBitmap()) }
    }

    override suspend fun renderPageFragment(
        index: Int,
        pageSize: Rect,
        scaledFragment: Rect,
        scale: Float,
        onComplete: (ScaledPage) -> Unit
    ) {
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

    private fun createBitmap(width: Float, height: Float) =
        Bitmap.createBitmap(width.roundToInt(), height.roundToInt(), Bitmap.Config.ARGB_8888)
}