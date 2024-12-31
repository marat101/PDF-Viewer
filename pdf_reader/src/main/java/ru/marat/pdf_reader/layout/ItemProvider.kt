package ru.marat.pdf_reader.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page
import ru.marat.viewplayground.pdf_reader.reader.layout.items.PageLayout

@OptIn(ExperimentalFoundationApi::class)
internal class ItemProvider(
    private val items: List<Page>
) : LazyLayoutItemProvider {
    override val itemCount: Int
        get() = items.size

    @Composable
    override fun Item(index: Int, key: Any) {
        val item = items.getOrNull(index)
        if (item != null) PageLayout(page = item)
    }
}

@Composable
internal fun rememberPagesItemProvider(
    items: List<Page>
): ItemProvider {
    return remember(items) { ItemProvider(items) }
}
