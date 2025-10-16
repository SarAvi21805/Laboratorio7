package com.example.ejercicioenclase2708.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ejercicioenclase2708.data.remote.PexelsPhoto
import java.util.Date

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: Long,
    val width: Int,
    val height: Int,
    val url: String, // URL grande para detalles
    val thumbnailUrl: String, // URL pequeña para la grilla
    val photographer: String,
    val alt: String,

    // Campos para la persistencia
    var isFavorite: Boolean = false,
    val queryKey: String, // La consulta de búsqueda normalizada a la que pertenece
    val pageIndex: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

// Función de mapeo para convertir el modelo de red al de la base de datos
fun PexelsPhoto.toEntity(query: String, page: Int): PhotoEntity {
    return PhotoEntity(
        id = this.id,
        width = this.width,
        height = this.height,
        url = this.src.large2x ?: this.src.original,
        thumbnailUrl = this.src.medium ?: this.src.small ?: "",
        photographer = this.photographer,
        alt = this.alt ?: "Photo by ${this.photographer}",
        queryKey = query.lowercase().trim(),
        pageIndex = page
    )
}