package com.example.ejercicioenclase2708.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("SELECT * FROM photos WHERE queryKey = :query ORDER BY pageIndex ASC, id ASC")
    fun getPhotosByQuery(query: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    fun getPhotoById(photoId: Long): Flow<PhotoEntity?>

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id = :photoId")
    suspend fun setFavorite(photoId: Long, isFavorite: Boolean)

    @Query("DELETE FROM photos WHERE queryKey = :query")
    suspend fun clearPhotosByQuery(query: String)

    @Query("SELECT * FROM photos WHERE id IN (:ids) AND isFavorite = 1")
    suspend fun getFavoritePhotosByIds(ids: Set<Long>): List<PhotoEntity>

    // Obtener fotos favoritas
    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePhotos(): Flow<List<PhotoEntity>>

    // Obtener fotos por nombre del fotógrafo (autor)
    @Query("SELECT * FROM photos WHERE photographer = :authorName ORDER BY id DESC")
    fun getPhotosByAuthor(authorName: String): Flow<List<PhotoEntity>>

    // Para consultar en la base de datos sincronamente
    @Query("SELECT * FROM photos WHERE id = :id")
    fun getPhotoByIdSync(id: Long): PhotoEntity?
}