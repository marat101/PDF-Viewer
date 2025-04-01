package ru.marat.pdf_reader.layout.saver

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.unit.Density
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.marat.pdf_reader.gestures.ReaderLayoutPositionState
import ru.marat.pdf_reader.layout.state.ReaderState
import ru.marat.pdf_reader.utils.Anchor
import ru.marat.pdf_reader.utils.cache.PdfViewerCache
import ru.marat.pdf_reader.utils.createAnchor
import ru.marat.pdf_reader.utils.pdf_info.PdfInfoProvider


class ReaderLayoutPositionSaver(
    private val minZoom: Float,
    private val maxZoom: Float,
    private val density: Density) :
    Saver<ReaderLayoutPositionState, String> {
    override fun restore(value: String): ReaderLayoutPositionState? {
        val value = Json.decodeFromString(RestoreData.serializer(), value)
        return ReaderLayoutPositionState(
            density = density,
            anchor = value.anchor,
            orientation = value.orientation,
            minZoom = minZoom,
            maxZoom = maxZoom
        )
    }

    override fun SaverScope.save(value: ReaderLayoutPositionState): String? {
        val layoutInfo = value.layoutInfo.value
        val restoreData = RestoreData(
            anchor = createAnchor(layoutInfo),
            orientation = layoutInfo.orientation
        )
        return Json.encodeToString(restoreData)
    }

    @Serializable
    data class RestoreData(
        val anchor: Anchor?,
        val orientation: Orientation
    )
}

internal class ReaderSaver(
    private val provider: PdfInfoProvider,
    private val scrollState: ReaderLayoutPositionState,
    private val cache: PdfViewerCache?
) : Saver<ReaderState, List<String>> {
    override fun restore(value: List<String>): ReaderState {
        val value = value.map { Json.decodeFromString(PageData.serializer(), it) }
        return ReaderState(
            pdfViewerCache = cache,
            positionsState = scrollState,
            pdfInfoProvider = provider,
            savedPages = value.ifEmpty { null },
        )
    }

    override fun SaverScope.save(value: ReaderState): List<String> {
        return value.positionsState.layoutInfo.value.pages.map { page ->
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
