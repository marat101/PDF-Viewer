package ru.marat.pdfreader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

object FileDownloader {
    private val client = HttpClient(OkHttp) {
        Logging {
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor: $message")
                }
            }
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun downloadFile(
        url: String,
        name: String? = null,
        context: Context,
        onProgress: (Float) -> Unit
    ): Uri? =
        withContext(Dispatchers.IO) {
            val fileName = (name ?: System.currentTimeMillis().toString()).trim() + ".pdf"
            println("marat $fileName")
            val downloads = context.cacheDir
            val file = File("$downloads/$fileName")
            if (file.exists()) file.delete()
            file.createNewFile()
            client.prepareGet(url).execute { response ->
                val channel = response.bodyAsChannel()
                val size = response.contentLength() ?: 0L
                var dBytes = 0L
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(limit = 5000000)
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        file.appendBytes(bytes)
                        dBytes += bytes.size
                        withContext(Dispatchers.Main.immediate) {
                            onProgress(
                                (dBytes.toDouble() / size.toDouble()).toFloat().coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }
            val uri = copyFileToDownloads(context, file)
            file.delete()
            uri
        }

    private fun copyFileToDownloads(context: Context, downloadedFile: File): Uri? {
        val resolver = context.contentResolver
        val downloadDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/PdfReader"
        File(downloadDir).let { directory ->
            if (!directory.exists() || !directory.isDirectory) directory.mkdir()
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, downloadedFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.SIZE, downloadedFile.length())
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/PdfReader")
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val authority = "${context.packageName}.provider"
            val destinyFile = File("$downloadDir/PdfReader", downloadedFile.name)
            FileProvider.getUriForFile(context, authority, destinyFile)
        }?.also { downloadedUri ->
            resolver.openOutputStream(downloadedUri).use { outputStream ->
                val brr = ByteArray(1024)
                var len: Int
                val bufferedInputStream =
                    BufferedInputStream(FileInputStream(downloadedFile.absoluteFile))
                while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                    outputStream?.write(brr, 0, len)
                }
                outputStream?.flush()
                bufferedInputStream.close()
            }
        }
    }
}