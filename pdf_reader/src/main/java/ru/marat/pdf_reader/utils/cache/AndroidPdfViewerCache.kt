package ru.marat.pdf_reader.utils.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.json.Json
import ru.marat.pdf_reader.layout.saver.PageData
import ru.marat.pdf_reader.utils.cache.PDFViewerCache.Companion.CACHE_ROOT_PACKAGE
import java.io.File
import java.io.FileOutputStream

@Immutable
class PDFViewerCacheImpl(
    private val context: Context,
    private val key: CacheKey
) : PDFViewerCache {

    private val directoryByKey: File
        get() = File(context.cacheDir, "$CACHE_ROOT_PACKAGE/cache_${key.value}/").apply {
            if (!exists()) mkdirs()
        }

    init {
        println(directoryByKey.absolutePath)
    }
    private val boundariesFile
        get() = File(directoryByKey, "boundaries.json")

    override suspend fun saveBoundaries(boundaries: List<PageData>) {
        if (!boundariesFile.exists()) boundariesFile.createNewFile()
        File(directoryByKey, "boundaries.json").writeText(Json.encodeToString(boundaries))
    }

    override suspend fun getBoundaries(): List<PageData>? {
        if (!boundariesFile.exists()) return null
        val str = File(directoryByKey, "boundaries.json").readText()
        return Json.decodeFromString(str)
    }

    override suspend fun savePage(index: Int, bm: ImageBitmap) {
        val file = File(directoryByKey, "image_$index.png")
        saveBitmapToFile(bm.asAndroidBitmap(), file)
    }

    override suspend fun getPage(index: Int): ImageBitmap? {
        val file = File(directoryByKey, "image_$index.png")
        return loadBitmapFromFile(file)?.asImageBitmap()
    }

    override suspend fun clear() {
        directoryByKey.deleteRecursively()
    }

    private fun saveBitmapToFile(
        bitmap: Bitmap,
        file: File,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Boolean {
        return try {
            if (file.exists()) file.delete()
            file.createNewFile()
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(format, quality, outputStream)
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            file.delete()
            e.printStackTrace()
            false
        }
    }

    private fun loadBitmapFromFile(file: File): Bitmap? {
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }
}

fun PDFViewerCache.clearAll(context: Context) =
    File(context.cacheDir, CACHE_ROOT_PACKAGE).deleteRecursively()

@Composable
fun rememberCache(key: CacheKey?): PDFViewerCache? {
    val context = LocalContext.current
    return remember(key) {
        if (key != null) PDFViewerCacheImpl(context, key)
        else null
    }
}