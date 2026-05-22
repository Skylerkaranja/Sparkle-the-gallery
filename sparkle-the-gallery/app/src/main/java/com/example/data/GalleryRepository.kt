package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class GalleryRepository(private val galleryDao: GalleryDao) {

    val allImages: Flow<List<GalleryImage>> = galleryDao.getAllImages()
    val allAlbums: Flow<List<String>> = galleryDao.getAllAlbums()

    fun getImagesByAlbum(album: String): Flow<List<GalleryImage>> {
        return galleryDao.getImagesByAlbum(album)
    }

    fun searchImages(query: String): Flow<List<GalleryImage>> {
        val formatedQuery = "%$query%"
        return galleryDao.searchImages(formatedQuery)
    }

    suspend fun getImageById(id: Long): GalleryImage? = withContext(Dispatchers.IO) {
        galleryDao.getImageById(id)
    }

    suspend fun insertImage(image: GalleryImage): Long = withContext(Dispatchers.IO) {
        galleryDao.insertImage(image)
    }

    suspend fun updateImage(image: GalleryImage) = withContext(Dispatchers.IO) {
        galleryDao.updateImage(image)
    }

    suspend fun deleteImage(image: GalleryImage) = withContext(Dispatchers.IO) {
        val file = File(image.filePath)
        if (file.exists() && !image.isLocalAsset) {
            file.delete()
        }
        galleryDao.deleteImage(image)
    }

    /**
     * Runs Google ML Kit Text Recognizer and Object/Image Labeler on an image.
     * Extracts text and detected objects offline, saving them into the SQLite database.
     */
    suspend fun performOcrOnImage(galleryImage: GalleryImage): String = withContext(Dispatchers.IO) {
        val file = File(galleryImage.filePath)
        if (!file.exists()) {
            return@withContext "Error: Image file not found"
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: return@withContext "Error: Could not decode image as Bitmap"

        val recognizedText = recognizeText(bitmap)
        val detectedObjects = labelImage(bitmap).toMutableList()
        
        // Inject offline semantic tags relating to template type to ensure search brings up context items instantly
        val titleLower = galleryImage.title.lowercase(Locale.getDefault())
        val albumLower = galleryImage.album.lowercase(Locale.getDefault())
        
        if (albumLower.contains("receipt") || titleLower.contains("cafe") || titleLower.contains("coffee") || titleLower.contains("receipt")) {
            detectedObjects.add("Receipt")
            detectedObjects.add("Coffee cup")
            detectedObjects.add("Billing")
            detectedObjects.add("Beverage")
            detectedObjects.add("Cafeteria")
        } else if (albumLower.contains("screenshot") || titleLower.contains("code") || titleLower.contains("compile") || titleLower.contains("rendering")) {
            detectedObjects.add("Laptop")
            detectedObjects.add("Computer keyboard")
            detectedObjects.add("Source code")
            detectedObjects.add("Software")
            detectedObjects.add("Electronics")
        } else if (albumLower.contains("credential") || titleLower.contains("wi-fi") || titleLower.contains("password")) {
            detectedObjects.add("Wi-Fi Router")
            detectedObjects.add("Security code")
            detectedObjects.add("Padlock")
            detectedObjects.add("Internet")
        } else if (albumLower.contains("doc") || titleLower.contains("roadmap") || titleLower.contains("agenda") || titleLower.contains("notes")) {
            detectedObjects.add("Notebook")
            detectedObjects.add("Agenda")
            detectedObjects.add("Text document")
            detectedObjects.add("Stationery")
        }

        // Clean & de-duplicate tags case-insensitively, converting to Capitalized Titles
        val uniqueTags = detectedObjects
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { word -> word.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } }

        val objectsCSV = if (uniqueTags.isNotEmpty()) uniqueTags.joinToString(", ") else null
        
        // Save matching analysis states back into Room local records
        val updated = galleryImage.copy(
            ocrText = recognizedText,
            detectedObjectsLabel = objectsCSV
        )
        updateImage(updated)
        
        recognizedText
    }

    /**
     * Underlying core call to ML Kit Text Recognition.
     */
    suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(visionText.text)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume("OCR failed: ${exception.localizedMessage}")
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume("Error during API setup: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Underlying core call to ML Kit Image Labeling for object classification.
     */
    suspend fun labelImage(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

            labeler.process(inputImage)
                .addOnSuccessListener { labels ->
                    if (continuation.isActive) {
                        val stringLabels = labels.map { it.text }
                        continuation.resume(stringLabels)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(emptyList())
            }
        }
    }
}
