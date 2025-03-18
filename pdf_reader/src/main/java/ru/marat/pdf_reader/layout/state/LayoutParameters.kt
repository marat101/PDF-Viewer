package ru.marat.pdf_reader.layout.state

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class LayoutParams(
    val viewportSize: Size = Size.Unspecified,
    val spacing: Float = 0F,
    val orientation: Orientation,
    val fullSize: Size = Size.Zero
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

@Stable
abstract class LayoutParameters(
    orientation: Orientation,
) : PagePositionController {

    private val _layoutParams = MutableStateFlow(
        LayoutParams(
            orientation = orientation,
        )
    )
    val layoutParams: StateFlow<LayoutParams>
        get() = _layoutParams


    internal fun updateLayoutParams(
        viewportSize: Size = _layoutParams.value.viewportSize,
        spacing: Float = _layoutParams.value.spacing,
        fullSize: Size = _layoutParams.value.fullSize,
        orientation: Orientation = _layoutParams.value.orientation
    ) {
        _layoutParams.update {
            it.copy(
                spacing = spacing,
                viewportSize = viewportSize,
                fullSize = fullSize,
                orientation = orientation
            )
        }
    }
}