package ru.marat.pdfreader.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(
    orientation: Orientation,
    onOrientationChange: (Orientation) -> Unit,
) {
    val density = LocalDensity.current
    val bottomPadding = density.run { WindowInsets.systemBars.getBottom(this).toDp() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = {
            onOrientationChange(
                if (orientation == Orientation.Horizontal) Orientation.Vertical
                else Orientation.Horizontal
            )
        }) {
            Text("Change orientation")
        }
        Spacer(Modifier.size(8.dp))
        Text("Orientation:\n$orientation")
    }
}