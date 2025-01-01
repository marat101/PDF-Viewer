package ru.marat.pdfreader.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun MainToolbar(
    offsetY: Float,
    onGetFileUri: (Uri) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    var visible by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .background(Color.White)
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(65.dp)
            .zIndex(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable {
                visible = !visible
            }) {
            Text(
                modifier = Modifier.align(Alignment.CenterStart),
                text = "Pdf Reader",
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 18.sp
            )
        }
        if (visible)
            PdfSelectionPopUp(
                onGetFileUri = onGetFileUri
            ) { visible = false }
        Text("offset:\n${offsetY.roundToInt()}")
    }
}