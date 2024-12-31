package ru.marat.pdf_reader.utils.pdf_info

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
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

    override val pageRenderer: PageRenderer = AndroidPageRenderer(renderer)
    override val pageCount: Int = renderer.pageCount

    override fun getPageAspectRatio(index: Int): Float {
        val page = renderer.openPage(index)
        val ratio = page.height.toFloat() / page.width.toFloat()
        page.close()
        return ratio
    }

    override fun close() {
        renderer.close()
    }
}

class AndroidPdfInfoProvider(
    private val context: Context,
    private val uri: Uri
) : PdfInfoProvider {
    override suspend fun get(): PdfInfo = AndroidPdfInfo.create(context, uri)
    override fun toString(): String {
        return uri.toString()
    }
}