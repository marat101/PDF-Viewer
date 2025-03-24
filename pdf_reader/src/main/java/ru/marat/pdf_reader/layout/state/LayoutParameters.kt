package ru.marat.pdf_reader.layout.state

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@Immutable
data class LayoutInfo(
    val viewportSize: Size = Size.Unspecified,
    val spacing: Float = 0F,
    val orientation: Orientation,
    val fullSize: Size = Size.Zero,
    val pages: List<Page> = emptyList(),
    val pagePositions: List<PagePosition> = emptyList(),
    val offset: Offset = Offset.Zero,
) {

    val fullHeight: Float
        get() = fullSize.height
    val fullWidth: Float
        get() = fullSize.width

    val offsetX get() = offset.x
    val offsetY get() = offset.y

    val isVertical get() = orientation == Orientation.Vertical

    fun calculateMaxOffsetY(): Float =
        if (viewportSize.isSpecified)
            (fullHeight - viewportSize.height).coerceAtLeast(0f)
        else 0f


    fun calculateMaxOffsetX(): Float =
        if (viewportSize.isSpecified)
            (fullWidth - viewportSize.width).coerceAtLeast(0f)
        else 0f

    val loadedPages: List<PagePosition>
        get() {
            if (viewportSize.isUnspecified) return emptyList()
            return pagePositions.filter {
                if (isVertical) {
                    val r = viewportSize.toRect().translate(0f, -offset.y)
                    r.overlaps(Rect(0f, it.start, 1f, it.end))
                } else {
                    val r = viewportSize.toRect().translate(-offset.x, 0f)
                    r.overlaps(Rect(it.start, 0f, it.end, 1f))
                }
            }
        }
}