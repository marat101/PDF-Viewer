package ru.marat.pdf_reader.utils.pdf_info

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.marat.pdf_reader.utils.render.AndroidPageRenderer
import ru.marat.pdf_reader.utils.render.PageRenderer

class AndroidPdfInfo(
    private val renderer: RendererScope,
    override val pageCount: Int,
) : PdfInfo {

    companion object {
        suspend fun create(context: Context, uri: Uri): AndroidPdfInfo =
            withContext(Dispatchers.IO) {
                val scope = RendererScope {
                    PdfRenderer(context.contentResolver.openFileDescriptor(uri, "r")!!)
                }
                val pageCount = scope.use {
                    it.pageCount
                }
                AndroidPdfInfo(scope, pageCount)
            }
    }

    override val pageRenderer: PageRenderer = AndroidPageRenderer(renderer)

    override suspend fun getPageAspectRatio(index: Int): Float {
        return renderer.use { renderer ->
            val page = renderer.openPage(index)
            val ratio = page.height.toFloat() / page.width.toFloat()
            page.close()
            ratio
        }
    }
}

class AndroidPdfInfoProvider(
    private val context: Context,
    private val uri: Uri
) : PdfInfoProvider {
    override suspend fun get(): PdfInfo = AndroidPdfInfo.create(context, uri)
}