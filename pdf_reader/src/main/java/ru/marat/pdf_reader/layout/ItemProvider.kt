package ru.marat.pdf_reader.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.marat.pdf_reader.items.Page
import ru.marat.viewplayground.pdf_reader.reader.layout.items.PageLayout

@OptIn(ExperimentalFoundationApi::class)
internal class ItemProvider(
    private val items: List<Page>,
    private val itemsOverlay: @Composable BoxScope.(Page) -> Unit
) : LazyLayoutItemProvider {
    override val itemCount: Int
        get() = items.size

    @Composable
    override fun Item(index: Int, key: Any) {
        val item = items.getOrNull(index)
        if (item != null) PageLayout(page = item, overlay = itemsOverlay)
    }
}

@Composable
internal fun rememberPagesItemProvider(
    items: List<Page>,
    itemsOverlay: @Composable BoxScope.(Page) -> Unit
): ItemProvider {
    return remember(items, itemsOverlay) { ItemProvider(items, itemsOverlay) }
}
