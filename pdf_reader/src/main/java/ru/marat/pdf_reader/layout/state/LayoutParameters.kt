package ru.marat.pdf_reader.layout.state

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@Immutable
data class LayoutInfo(
    val viewportSize: Size = Size.Unspecified,
    val spacing: Float = 0F,
    val orientation: Orientation,
    val fullSize: Size = Size.Zero,
    val pages: List<Page> = emptyList(),
    val pagePositions: List<PagePosition> = emptyList(),
) {

    val fullHeight: Float
        get() = fullSize.height
    val fullWidth: Float
        get() = fullSize.width

    val isVertical get() = orientation == Orientation.Vertical

    fun calculateMaxOffsetY(): Float =
        if (viewportSize.isSpecified)
            (fullHeight - viewportSize.height).coerceAtLeast(0f)
        else 0f


    fun calculateMaxOffsetX(): Float =
        if (viewportSize.isSpecified)
            (fullWidth - viewportSize.width).coerceAtLeast(0f)
        else 0f

}