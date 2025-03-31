package ru.marat.pdfreader.screen

import android.text.TextUtils.replace
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import okhttp3.internal.cache2.Relay.Companion.edit

@Composable
fun ScrollToPageDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onScroll: (Int) -> Boolean
) {
    if (visible) {
        val text = remember { mutableStateOf("") }
        val isError = remember { mutableStateOf(false) }
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text.value,
                    onValueChange = {
                        isError.value = false
                        text.value = it
                    },
                    label = {
                        Text("Page")
                    },
                    isError = isError.value,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            isError.value = !onScroll(text.value.toInt())
                        }
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        isError.value = !onScroll(text.value.toInt())
                    }
                ) {
                    Text("Scroll")
                }
            }
        }
    }
}