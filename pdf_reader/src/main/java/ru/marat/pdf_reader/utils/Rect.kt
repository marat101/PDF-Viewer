package ru.marat.pdf_reader.utils

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import ru.marat.viewplayground.pdf_reader.reader.layout.items.toIntOffset

fun Rect.toIntRect() = IntRect(topLeft.toIntOffset(), bottomRight.toIntOffset())

fun Size.toIntSize() = IntSize(width.toInt(), height.toInt())