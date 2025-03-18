package ru.marat.pdf_reader.layout.saver

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.marat.pdf_reader.gestures.ReaderLayoutPositionState
import ru.marat.pdf_reader.layout.state.ReaderState
import ru.marat.pdf_reader.utils.pdf_info.PdfInfoProvider


class ReaderLayoutPositionSaver(private val density: Density) :
    Saver<ReaderLayoutPositionState, String> {
    override fun restore(value: String): ReaderLayoutPositionState? {
        val value = Json.decodeFromString(RestoreData.serializer(), value)
        return ReaderLayoutPositionState(
            density = density,
            offsetFraction = value.offsetFraction,
            previousOffset = Offset(value.offsetX, value.offsetY),
            orientation = value.orientation
        )
    }

    override fun SaverScope.save(value: ReaderLayoutPositionState): String {
        return Json.encodeToString(
            RestoreData(
                offsetFraction = 0f,
                offsetX = value.offsetX,
                offsetY = value.offsetY,
                orientation = Orientation.Vertical
            )
        )
    }

    @Serializable
    data class RestoreData(
        val offsetFraction: Float, //fixme
        val offsetX: Float,
        val offsetY: Float,
        val orientation: Orientation
    )
}

internal class ReaderSaver(
    private val provider: PdfInfoProvider,
    private val scrollState: ReaderLayoutPositionState
) : Saver<ReaderState, List<String>> {
    override fun restore(value: List<String>): ReaderState {
        val value = value.map { Json.decodeFromString(PageData.serializer(), it) }
        return ReaderState(
            positionsState = scrollState,
            pdfInfoProvider = provider,
            savedPages = value.ifEmpty { null },
        )
    }

    override fun SaverScope.save(value: ReaderState): List<String> {
        return value.positionsState.pages.value.map { page ->
            Json.encodeToString(
                PageData.serializer(),
                PageData(
                    index = page.index,
                    ratio = page.ratio
                )
            )
        }
    }
}

@Serializable
data class PageData(
    val index: Int,
    val ratio: Float,
)
