package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_images")
data class GalleryImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val title: String,
    val album: String,
    val timestamp: Long,
    val ocrText: String? = null,
    val detectedObjectsLabel: String? = null,
    val isLocalAsset: Boolean = false
)
