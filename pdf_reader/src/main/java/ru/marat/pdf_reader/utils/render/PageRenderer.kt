package ru.marat.pdf_reader.utils.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import ru.marat.viewplayground.pdf_reader.reader.layout.items.ScaledPage

interface PageRenderer {

    suspend fun renderPage(
        index: Int,
        pageSize: Rect,
        onComplete: (ImageBitmap) -> Unit
    )
    suspend fun renderPageFragment(
        index: Int,
        pageSize: Rect,
        scaledFragment: Rect,
        scale: Float,
        onComplete: (ScaledPage) -> Unit
    )
}