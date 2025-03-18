package ru.marat.pdf_reader.items

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page
import kotlin.math.roundToInt

//internal fun MeasureScope.placeByLayoutOrientation(
//    page: Page,
//    constraints: Constraints,
//    measurables: List<Measurable>
//): MeasureResult =
//    if (page.layoutHelper.isVertical) placeVerticalLayoutPage(
//        page.layoutHelper.parentViewportSize,
//        page,
//        constraints,
//        measurables
//    )
//    else placeHorizontalLayoutPage(page, constraints, measurables)


private fun MeasureScope.placeVerticalLayoutPage(
    pageSize: Size,
    page: Page,
    constraints: Constraints,
    measurables: List<Measurable>
): MeasureResult {

    val height = (constraints.maxWidth.toFloat() * page.ratio).roundToInt() // todo вынести в layoutHelper
    val width = constraints.maxWidth
//    val height = page.size.value.height
    val placeables = measurables.fastMap { measurable ->
        measurable.measure(
            constraints.copy(
                minHeight = height
            )
        )
    }
//    page.viewportSize = Size(width.toFloat(), height.toFloat())
    return layout(width, height) {
        placeables.fastForEach {
            val x = (height / 2) - (it.height / 2)
            val y = (width / 2) - (it.width / 2)
            it.place(x, y)
        }
    }
}

private fun MeasureScope.placeHorizontalLayoutPage(
    page: Page,
    constraints: Constraints,
    measurables: List<Measurable>
): MeasureResult {
    val pos = page.layoutHelper.getPositionByIndex(page.index)
    val width = if (pos == null) constraints.maxWidth
    else (pos.end - pos.start).roundToInt()
    val height = (width * page.ratio)
        .coerceAtMost(constraints.maxHeight.toFloat())
        .roundToInt()
    val placeables = measurables.fastMap { measurable ->
        measurable.measure(
            constraints.copy(
                minHeight = height,
                minWidth = width,
                maxWidth = width
            )
        )
    }
    return layout(width, height) {
        placeables.fastForEach {
            val x = 0
            val y = 0
            it.placeRelative(x, y)
        }
    }
}