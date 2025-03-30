package ru.marat.pdfreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import ru.marat.pdfreader.screen.MainScreen
import ru.marat.viewplayground.pdf_reader.reader.layout.items.LocalPageColors
import ru.marat.viewplayground.pdf_reader.reader.layout.items.PageColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalPageColors provides PageColors(backgroundColor = Color.White)
            ) {
                MainScreen()
            }
        }
    }
}