package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM gallery_images ORDER BY timestamp DESC")
    fun getAllImages(): Flow<List<GalleryImage>>

    @Query("SELECT * FROM gallery_images WHERE id = :id")
    suspend fun getImageById(id: Long): GalleryImage?

    @Query("SELECT * FROM gallery_images WHERE album = :album ORDER BY timestamp DESC")
    fun getImagesByAlbum(album: String): Flow<List<GalleryImage>>

    @Query("SELECT DISTINCT album FROM gallery_images")
    fun getAllAlbums(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GalleryImage): Long

    @Update
    suspend fun updateImage(image: GalleryImage)

    @Delete
    suspend fun deleteImage(image: GalleryImage)

    @Query("SELECT * FROM gallery_images WHERE title LIKE :query OR ocrText LIKE :query OR detectedObjectsLabel LIKE :query ORDER BY timestamp DESC")
    fun searchImages(query: String): Flow<List<GalleryImage>>
}
