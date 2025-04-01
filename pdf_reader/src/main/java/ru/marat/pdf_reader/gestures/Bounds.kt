package ru.marat.pdf_reader.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.ui.geometry.Offset

data class Bounds(
    val min: Float,
    val max: Float
) {
    constructor(value: Float) : this(-value, value)

    companion object {
        val Zero = Bounds(0f, 0f)
    }
}


fun Float.setBounds(bounds: Bounds) = this.coerceIn(bounds.min..bounds.max)

fun Offset.setOffsetBounds(horizontal: Bounds, vertical: Bounds): Offset {
    val newX = x.setBounds(horizontal)
    val newY = y.setBounds(vertical)
    return Offset(newX, newY)
}

fun Animatable<Float, AnimationVector1D>.updateBounds(bounds: Bounds) =
    updateBounds(bounds.min, bounds.max)