package ru.marat.pdf_reader.utils.pdf_info

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.marat.pdf_reader.utils.render.AndroidPageRenderer
import ru.marat.pdf_reader.utils.render.PageRenderer

class AndroidPdfInfo(
    override val pageCount: Int,
    onCreateRenderer: () -> PdfRenderer //todo оптимизировать использование отрисовщика
) : PdfInfo {

    companion object {
        suspend fun create(context: Context, uri: Uri): AndroidPdfInfo =
            withContext(Dispatchers.IO) {
                val onCreateRenderer = {
                    PdfRenderer(context.contentResolver.openFileDescriptor(uri, "r")!!)
                }
                val renderer = onCreateRenderer()
                val pageCount = renderer.pageCount
                renderer.close()
                AndroidPdfInfo(pageCount, onCreateRenderer)
            }
    }

    private val rendererScope = RendererScope(onCreateRenderer)

    override val pageRenderer: PageRenderer = AndroidPageRenderer(rendererScope)

    override suspend fun getPageAspectRatio(index: Int): Float {
        return rendererScope.use { renderer ->
            val page = renderer.openPage(index)
            val ratio = page.height.toFloat() / page.width.toFloat()
            page.close()
            ratio
        }
    }

    override fun close() {
//        CoroutineScope(Dispatchers.Default).launch {
//            mutex.withLock {
//                onCreateRenderer.close()
//            }
//        }
    }
}

class AndroidPdfInfoProvider(
    private val context: Context,
    private val uri: Uri
) : PdfInfoProvider {
    override suspend fun get(): PdfInfo = AndroidPdfInfo.create(context, uri)
}