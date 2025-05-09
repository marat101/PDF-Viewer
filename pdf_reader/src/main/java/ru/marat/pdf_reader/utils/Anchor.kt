package ru.marat.pdf_reader.utils

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import ru.marat.pdf_reader.layout.state.LayoutInfo
import kotlin.math.absoluteValue

@Immutable
@Serializable
data class Anchor(
    val pageIndex: Int,
    val offsetFraction: Float
)


fun createAnchor(layoutInfo: LayoutInfo): Anchor? {
    val firstVisiblePage = layoutInfo.visiblePages.firstOrNull() ?: return null
    val pageSize = firstVisiblePage.end.absoluteValue - firstVisiblePage.start.absoluteValue
    val l = (if (layoutInfo.isVertical) layoutInfo.offsetY.absoluteValue
    else layoutInfo.offsetX.absoluteValue) - firstVisiblePage.start.absoluteValue
    return Anchor(
        pageIndex = firstVisiblePage.index,
        offsetFraction = l / pageSize
    )
}