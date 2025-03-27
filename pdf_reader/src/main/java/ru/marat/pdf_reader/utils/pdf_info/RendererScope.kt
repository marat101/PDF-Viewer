package ru.marat.pdf_reader.utils.pdf_info

import android.graphics.pdf.PdfRenderer
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicInteger

class RendererScope(
    private val onCreate: () -> PdfRenderer,
) {
    private val mutex = Mutex()

    private val operationsCount = AtomicInteger(0)
    private var renderer: PdfRenderer? = null

    suspend fun <T> use(block: suspend (PdfRenderer) -> T): T  {
        operationsCount.incrementAndGet()

        mutex.lock()
        if (renderer == null) renderer = onCreate()

        val onClose = {
            val currentOpCount = operationsCount.decrementAndGet()
            if (currentOpCount <= 0) {
                renderer?.close()
                renderer = null
            }
            mutex.unlock()
        }
        val result = runCatching { block(renderer!!) }.getOrElse {
            onClose()
            throw it
        }
        onClose()
        return result
    }
}