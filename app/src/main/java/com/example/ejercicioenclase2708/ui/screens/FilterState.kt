package com.example.ejercicioenclase2708.ui.screens

import androidx.compose.runtime.saveable.Saver

// Usamos una sealed class para representar los posibles estados del filtro.
sealed class FilterState {
    object None : FilterState()
    object FavoritesOnly : FilterState()
    data class ByAuthor(val authorName: String) : FilterState()
}

// Saver para que rememberSaveable pueda guardar y restaurar nuestro FilterState personalizado.
val filterStateSaver = Saver<FilterState, String>(
    save = {
        when (it) {
            FilterState.None -> "none"
            FilterState.FavoritesOnly -> "favorites"
            is FilterState.ByAuthor -> "author:${it.authorName}"
        }
    },
    restore = {
        when {
            it.startsWith("author:") -> FilterState.ByAuthor(it.removePrefix("author:"))
            it == "favorites" -> FilterState.FavoritesOnly
            else -> FilterState.None
        }
    }
)