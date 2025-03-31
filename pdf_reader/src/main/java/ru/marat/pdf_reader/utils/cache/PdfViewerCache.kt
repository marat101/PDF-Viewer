package ru.marat.pdf_reader.utils.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Stable
import kotlinx.serialization.json.Json
import ru.marat.pdf_reader.layout.saver.PageData
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.map

@Stable
class PdfViewerCache(
    context: Context,
    private val path: String
) {

    companion object {
        private const val DIRECTORY = "ru.marat.pdf_viewer.cache"
        private const val BOUNDARIES = "boundaries.json"
    }

    private val directory = File(context.cacheDir.path + "/$DIRECTORY/$path/")
    private val boundariesFile = File(directory, BOUNDARIES)
    init {
        directory.mkdirs()
    }

    fun saveBoundaries(
        pages: List<Page>
    ) {
        val boundaries = pages.map { page ->
            PageData(
                index = page.index,
                ratio = page.ratio
            )
        }
        val boundariesString = Json.encodeToString(boundaries)
        boundariesFile.writeText(boundariesString)
    }

    fun loadBoundaries(): List<PageData>? {
        return if (boundariesFile.exists()) {
            val boundariesString = boundariesFile.readText()
            Json.decodeFromString(boundariesString)
        } else null
    }

    fun savePage(index: Int, bitmap: Bitmap) {
        val imageFile = File(
            directory,
            "$index$index$index.png",
        )
        saveBitmapToFile(bitmap, imageFile)
    }

    fun getPage(index: Int): Bitmap? {

        val imageFile = File(directory, "$index$index$index.png")
        return loadBitmapFromFile(imageFile)
    }

    fun clear() {
        directory.deleteRecursively()
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