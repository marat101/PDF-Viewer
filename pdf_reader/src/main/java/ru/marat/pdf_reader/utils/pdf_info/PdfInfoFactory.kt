package ru.marat.pdf_reader.utils.pdf_info

import androidx.compose.runtime.Stable

@Stable
interface PdfInfoFactory {
    suspend fun create(): PdfInfo
}