package ru.marat.pdf_reader.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D

data class Bounds(
    val min: Float,
    val max: Float
) {
    companion object {
        val Zero = Bounds(0f, 0f)
    }
}


fun Float.setBounds(bounds: Bounds) = this.coerceIn(bounds.min..bounds.max)
fun Animatable<Float, AnimationVector1D>.updateBounds(bounds: Bounds) = updateBounds(bounds.min,bounds.max)