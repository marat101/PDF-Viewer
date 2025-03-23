package ru.marat.pdfreader.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.marat.pdfreader.FileDownloader
import java.io.File

@Composable
fun PdfSelectionPopUp(
    onGetFileUri: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var dialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onGetFileUri(uri)
            onDismiss()
        } else Toast.makeText(context, "No file selected", Toast.LENGTH_SHORT).show()
    }
    Popup(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .padding(top = statusBarPadding.calculateTopPadding())
                .shadow(8.dp)
                .background(Color.White)
        ) {
            Text(
                modifier = Modifier
                    .clickable {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                    .padding(10.dp),
                fontSize = 16.sp,
                text = "Open PDF from file system"
            )
            Text(
                modifier = Modifier
                    .clickable {
                        dialog = true
                    }
                    .padding(10.dp),
                fontSize = 16.sp,
                text = "Download PDF from url"
            )
        }
    }

    if (dialog)
        PdfDownloadDialog(
            onDismiss = {
                if (it != null) onGetFileUri(it)
                dialog = false
            }
        )
}

@Composable
private fun PdfDownloadDialog(
    onDismiss: (Uri?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var urlText by remember { mutableStateOf("http://") }
    var fileNameText by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss(null) },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = urlText,
                enabled = progress == null,
                onValueChange = {
                    error = false
                    urlText = it
                },
                isError = error,
                trailingIcon = {
                    if (progress == null)
                        Text(
                            modifier = Modifier
                                .clickable {
                                    progress = 0f
                                    scope.launch {
                                        runCatching {
                                            val file =
                                                FileDownloader.downloadFile(
                                                    urlText,
                                                    fileNameText.takeIf { it.isNotBlank() },
                                                    context,
                                                ) {
                                                    progress = it
                                                }
                                            progress = null
                                            onDismiss(file)
                                        }.getOrElse {
                                            it.printStackTrace()
                                            progress = null
                                            error = true
                                        }
                                    }
                                }
                                .padding(4.dp),
                            text = "Download",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    else CircularProgressIndicator(
                        progress = {
                            progress!!
                        },
                    )
                }
            )
            Spacer(Modifier.size(10.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = fileNameText,
                enabled = progress == null,
                onValueChange = { fileNameText = it }
            )
            Spacer(Modifier.size(10.dp))
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    scope.cancel()
                    onDismiss(null)
                }) {
                Text("Cancel")
            }
        }
    }
}