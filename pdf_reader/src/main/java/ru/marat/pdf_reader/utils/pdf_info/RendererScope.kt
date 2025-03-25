package ru.marat.pdf_reader.utils.pdf_info

import android.graphics.pdf.PdfRenderer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

class RendererScope(
    private val onCreate: () -> PdfRenderer,
) {
    private val mutex = Mutex()

    private val opNum = AtomicInteger(0)
    private var renderer: PdfRenderer? = null
    private val operations = mutableListOf<Int>()

    suspend fun <T> use(block: suspend (PdfRenderer) -> T): T = coroutineScope {
        val op = opNum.incrementAndGet()
        operations.add(op)
        mutex.lock()
        if (renderer == null) renderer = onCreate()

        val onClose = {
            operations.remove(element = op)
            if (operations.isEmpty()) {
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
        result
    }
}