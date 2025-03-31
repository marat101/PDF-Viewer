package ru.marat.pdf_reader.utils

import kotlinx.serialization.Serializable
import ru.marat.pdf_reader.layout.state.LayoutInfo
import kotlin.math.absoluteValue

@Serializable
data class Anchor(
    val previousFirstVisiblePage: Int,
    val fraction: Float
)


fun createAnchor(layoutInfo: LayoutInfo): Anchor? {
    val firstVisiblePage = layoutInfo.visiblePages.firstOrNull() ?: return null
    val pageSize = firstVisiblePage.end.absoluteValue - firstVisiblePage.start.absoluteValue
    val l = (if (layoutInfo.isVertical) layoutInfo.offsetY.absoluteValue
    else layoutInfo.offsetX.absoluteValue) - firstVisiblePage.start.absoluteValue
    return Anchor(
        previousFirstVisiblePage = firstVisiblePage.index,
        fraction = l / pageSize
    )
}