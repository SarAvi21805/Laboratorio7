package com.example.ejercicioenclase2708.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) // Corredor de pruebas para Android
class PhotoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var photoDao: PhotoDao

    // Ejecutando antes de cada prueba
    @Before
    fun setup() {
        // Creación de base de datos en memoria temporal, rápida y se destruye al acabar la prueba
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build() // Se permiten consultas en el hilo principal para pruebas

        photoDao = database.photoDao()
    }

    // Ejecutada después de cada prueba (desmontaje)
    @After
    fun teardown() {
        database.close() // Cierra la base de datos para liberar recursos.
    }

    // CASO DE PRUEBA: INSERTAR Y OBTENER FOTOS
    @Test
    fun insertAndGetPhotos_returnsCorrectData() = runBlocking {
        // Arrange: Prepara los datos de prueba
        val photo1 = PhotoEntity(1, 100, 100, "url1", "thumb1", "ph1", "alt1", false, "test", 1)
        val photo2 = PhotoEntity(2, 100, 100, "url2", "thumb2", "ph2", "alt2", false, "test", 1)
        val photoList = listOf(photo1, photo2)

        // Act: Ejecuta la acción a probar
        photoDao.insertPhotos(photoList)

        // Assert: Verificación del resultado
        val result = photoDao.getPhotosByQuery("test").first() // obtiene el primer valor emitido por el Flow

        assertEquals(2, result.size)
        assertEquals("alt1", result[0].alt)
        assertEquals("alt2", result[1].alt)
    }

    // CASO DE PRUEBA: MARCAR COMO FAVORITO
    @Test
    fun setFavorite_updatesPhotoCorrectly() = runBlocking {
        // Arrange
        val photo = PhotoEntity(1, 100, 100, "url1", "thumb1", "ph1", "alt1", false, "test", 1)
        photoDao.insertPhotos(listOf(photo))

        // Act
        photoDao.setFavorite(photoId = 1, isFavorite = true)

        // Assert
        val updatedPhoto = photoDao.getPhotoById(1).first()
        assertTrue(updatedPhoto?.isFavorite == true)
    }
}