package com.example.ejercicioenclase2708

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ejercicioenclase2708.data.local.AppDatabase
import com.example.ejercicioenclase2708.data.local.PhotoDao
import com.example.ejercicioenclase2708.data.local.PhotoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PhotoDaoTest {

    private lateinit var photoDao: PhotoDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        photoDao = db.photoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetPhotosByQuery() = runBlocking {
        // Arrange
        val photo1 = PhotoEntity(1, 100, 100, "", "", "Author A", "Nature Photo 1", false, "nature", 1)
        val photo2 = PhotoEntity(2, 100, 100, "", "", "Author B", "Nature Photo 2", false, "nature", 1)
        val photo3 = PhotoEntity(3, 100, 100, "", "", "Author A", "Car Photo", false, "cars", 1)

        photoDao.insertPhotos(listOf(photo1, photo2, photo3))

        // Act
        val naturePhotos = photoDao.getPhotosByQuery("nature").first()

        // Assert
        assertEquals(2, naturePhotos.size)
        assertTrue(naturePhotos.all { it.queryKey == "nature" })
    }

    @Test
    @Throws(Exception::class)
    fun setFavoriteAndGetFavorites() = runBlocking {
        // Arrange
        val photo1 = PhotoEntity(1, 100, 100, "", "", "Author A", "Photo 1", false, "query", 1)
        val photo2 = PhotoEntity(2, 100, 100, "", "", "Author B", "Photo 2", false, "query", 1)
        photoDao.insertPhotos(listOf(photo1, photo2))

        // Act
        photoDao.setFavorite(1, true)
        val favoritePhotos = photoDao.getFavoritePhotos().first()

        // Assert
        assertEquals(1, favoritePhotos.size)
        assertEquals(1L, favoritePhotos[0].id)
    }

    @Test
    @Throws(Exception::class)
    fun getPhotosByAuthor() = runBlocking {
        // Arrange
        val photo1 = PhotoEntity(1, 100, 100, "", "", "Author A", "Photo 1", false, "query1", 1)
        val photo2 = PhotoEntity(2, 100, 100, "", "", "Author B", "Photo 2", false, "query2", 1)
        val photo3 = PhotoEntity(3, 100, 100, "", "", "Author A", "Photo 3", false, "query3", 1)
        photoDao.insertPhotos(listOf(photo1, photo2, photo3))

        // Act
        val authorAPhotos = photoDao.getPhotosByAuthor("Author A").first()

        // Assert
        assertEquals(2, authorAPhotos.size)
        assertTrue(authorAPhotos.all { it.photographer == "Author A" })
    }
}