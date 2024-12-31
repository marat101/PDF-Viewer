package ru.marat.pdf_reader.layout.state

sealed interface LoadingState {
    data object Loading : LoadingState
    data object Ready : LoadingState
}