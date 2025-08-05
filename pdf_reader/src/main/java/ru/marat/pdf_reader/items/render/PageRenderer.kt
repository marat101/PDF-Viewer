package ru.marat.pdf_reader.items.render

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import ru.marat.pdf_reader.items.ScaledPage

interface PageRenderer {

    suspend fun renderPage(
        index: Int,
        pageSize: IntSize,
    ): ImageBitmap
    suspend fun renderPageFragment(
        index: Int,
        pageSize: IntRect,
        scaledFragment: IntRect,
        scale: Float,
    ): ScaledPage
}