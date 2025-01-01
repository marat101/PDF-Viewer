package ru.marat.pdf_reader.utils.pdf_info

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.marat.pdf_reader.utils.render.AndroidPageRenderer
import ru.marat.pdf_reader.utils.render.PageRenderer

class AndroidPdfInfo private constructor(
    private val renderer: PdfRenderer
) : PdfInfo {

    companion object {
        suspend fun create(context: Context, uri: Uri): AndroidPdfInfo =
            withContext(Dispatchers.IO) {
                val pdfRenderer =
                    PdfRenderer(context.contentResolver.openFileDescriptor(uri, "r")!!)
                AndroidPdfInfo(pdfRenderer)
            }
    }

    private val mutex = Mutex()

    override val pageRenderer: PageRenderer = AndroidPageRenderer(mutex, renderer)
    override val pageCount: Int = renderer.pageCount

    override fun getPageAspectRatio(index: Int): Float {
        val page = renderer.openPage(index)
        val ratio = page.height.toFloat() / page.width.toFloat()
        page.close()
        return ratio
    }

    override fun close() {
        CoroutineScope(Dispatchers.Default).launch {
            mutex.withLock {
                renderer.close()
            }
        }
    }
}

class AndroidPdfInfoProvider(
    private val context: Context,
    private val uri: Uri
) : PdfInfoProvider {
    override suspend fun get(): PdfInfo = AndroidPdfInfo.create(context, uri)
}