package ru.marat.pdf_reader.layout.state

sealed interface LoadingState {
    class Loading(val progress: Float) : LoadingState
    data object Ready : LoadingState
}