package com.example.ui

import android.content.Context
import android.graphics.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GalleryImage
import com.example.data.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel(private val repository: GalleryRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    private val _activeImageForDetail = MutableStateFlow<GalleryImage?>(null)
    val activeImageForDetail: StateFlow<GalleryImage?> = _activeImageForDetail.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _ocrResultText = MutableStateFlow<String?>(null)
    val ocrResultText: StateFlow<String?> = _ocrResultText.asStateFlow()

    private val _createImageSheetOpen = MutableStateFlow(false)
    val createImageSheetOpen: StateFlow<Boolean> = _createImageSheetOpen.asStateFlow()

    // Dynamically retrieve albums in real-time
    val allAlbums: StateFlow<List<String>> = repository.allAlbums
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe images with reactive searching and filtering
    val galleryImages: StateFlow<List<GalleryImage>> = combine(
        repository.allImages,
        _searchQuery,
        _selectedAlbum
    ) { images, query, album ->
        var result = images

        // 1. Filter by Album/Folder
        if (album != null) {
            result = result.filter { it.album.equals(album, ignoreCase = true) }
        }

        // 2. Search by Query (Case-Insensitive in Title, OCR text, or Detected Objects)
        if (query.isNotBlank()) {
            result = result.filter { image ->
                image.title.contains(query, ignoreCase = true) ||
                        (image.ocrText != null && image.ocrText.contains(query, ignoreCase = true)) ||
                        (image.detectedObjectsLabel != null && image.detectedObjectsLabel.contains(query, ignoreCase = true))
            }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAlbum(album: String?) {
        _selectedAlbum.value = album
    }

    fun viewImageDetail(image: GalleryImage?) {
        _activeImageForDetail.value = image
        _ocrResultText.value = image?.ocrText
    }

    fun setCreateImageSheetOpen(isOpen: Boolean) {
        _createImageSheetOpen.value = isOpen
    }

    /**
     * Re-scans or scans an image with local text recognition.
     */
    fun runOcrOnImage(image: GalleryImage) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            val text = repository.performOcrOnImage(image)
            _ocrResultText.value = text
            // Update active detail state with the updated entity content
            val updated = repository.getImageById(image.id)
            if (updated != null) {
                _activeImageForDetail.value = updated
            }
            _isOcrProcessing.value = false
        }
    }

    /**
     * Insert a custom user image generated natively.
     */
    fun insertCustomImage(
        context: Context,
        title: String,
        sampleType: String,
        albumName: String,
        textToDraw: String
    ) {
        viewModelScope.launch {
            val fName = "usr_${System.currentTimeMillis()}.png"
            val bitmap = createTextBitmap(title, textToDraw, sampleType)
            val path = saveBitmapToFile(context, bitmap, fName)

            val galleryImg = GalleryImage(
                filePath = path,
                title = title,
                album = albumName.trim() .ifBlank { "Scanned Text" },
                timestamp = System.currentTimeMillis()
            )

            val insertedId = repository.insertImage(galleryImg)
            val insertedImg = galleryImg.copy(id = insertedId)

            // Auto run ML Kit OCR on birth
            repository.performOcrOnImage(insertedImg)
        }
    }

    /**
     * Delete image from database and filesystem.
     */
    fun deleteImage(image: GalleryImage) {
        viewModelScope.launch {
            if (_activeImageForDetail.value?.id == image.id) {
                _activeImageForDetail.value = null
            }
            repository.deleteImage(image)
        }
    }

    /**
     * Pre-populates the database with sleek aesthetic images on first launch.
     */
    fun prepopulateDatabaseIfEmpty(context: Context) {
        viewModelScope.launch {
            repository.allImages.first().let { currentList ->
                if (currentList.isEmpty()) {
                    // Create beautiful high-contrast image bitmaps
                    val samples = listOf(
                        Quad(
                            "Office Wi-Fi Card",
                            "Creds",
                            "Office Wi-Fi\nNetwork: Frosted_HQ\nPassword: glass_crystal_2026\nEnjoy high-speed connection!",
                            "CREDENTIALS"
                        ),
                        Quad(
                            "Project Roadmap Notes",
                            "Agenda",
                            "PROJECT SERENE ROADMAP\n1. Dynamic Frosted Glass Themes\n2. Real-time OCR Text Analysis\n3. High density local DB Search",
                            "Documents"
                        ),
                        Quad(
                            "Cosmic Cafe Receipt",
                            "Receipt",
                            "COSMIC MARKET\n-------------------------\nSleek Latte       $4.50\nIndigo Muffin     $3.99\nFrosty Croissant  $5.12\n-------------------------\nTOTAL             $13.61",
                            "Receipts"
                        ),
                        Quad(
                            "Compose Custom Rendering",
                            "Code",
                            "// Jetpack Compose Rule\n@Composable\nfun FrostedCard(modifier: Modifier) {\n    Box(modifier = modifier\n        .blur(20.dp)\n        .background(Color.White.copy(0.2f))\n    )\n}",
                            "Screenshots"
                        )
                    )

                    samples.forEachIndexed { index, quad ->
                        val fileName = "sample_${index + 1}.png"
                        val bitmap = createTextBitmap(quad.title, quad.text, quad.type)
                        val path = saveBitmapToFile(context, bitmap, fileName)

                        val image = GalleryImage(
                            filePath = path,
                            title = quad.title,
                            album = quad.album,
                            timestamp = System.currentTimeMillis() - (index * 60000), // separate history
                            isLocalAsset = true
                        )

                        val insertedId = repository.insertImage(image)
                        val insertedImage = image.copy(id = insertedId)
                        // Trigger OCR immediately
                        repository.performOcrOnImage(insertedImage)
                    }
                }
            }
        }
    }

    private data class Quad(val title: String, val type: String, val text: String, val album: String)

    /**
     * Creates a high fidelity text bitmap with beautiful aesthetics matching Frosted Glass theme.
     */
    private fun createTextBitmap(title: String, content: String, type: String): Bitmap {
        val width = 800
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Select visual coloring depending on type
        val endColor = when (type.lowercase()) {
            "receipt" -> 0xFF312E81.toInt() // Deep Indigo
            "creds" -> 0xFF4338CA.toInt()   // Indigo Blue
            "code" -> 0xFF1E1B4B.toInt()    // Cosmic Black/Purple
            else -> 0xFF4F46E5.toInt()      // Smooth Indigo/Violet
        }
        val startColor = 0xFF581C87.toInt() // Rich Purple

        // Background Gradient
        val backPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                startColor, endColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backPaint)

        // Decorative Glassy Radial Accent Circles
        val accentPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(30, 232, 222, 248) // soft lavender
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width * 0.15f, height * 0.2f, width * 0.35f, accentPaint)
        canvas.drawCircle(width * 0.85f, height * 0.75f, width * 0.45f, accentPaint)

        // Subtle tech grid lines
        val gridPaint = Paint().apply {
            color = Color.argb(20, 255, 255, 255) // light white dash
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val step = height / 6f
        for (i in 1..5) {
            canvas.drawLine(0f, i * step, width.toFloat(), i * step, gridPaint)
            canvas.drawLine(i * step, 0f, i * step, height.toFloat(), gridPaint)
        }

        // Draw header badge
        val badgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val rectF = RectF(50f, 40f, 320f, 100f)
        canvas.drawRoundRect(rectF, 30f, 30f, badgePaint)

        val badgeTextPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFFE9D5FF.toInt() // purple-200
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("OCR ARCHIVE", 80f, 80f, badgeTextPaint)

        // Draw title heading
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(title, 60f, 170f, titlePaint)

        // Draw small horizontal separating neon-line
        val linePaint = Paint().apply {
            color = 0xFFA78BFA.toInt() // neon light violet
            strokeWidth = 4f
        }
        canvas.drawLine(60f, 200f, 260f, 200f, linePaint)

        // Draw core text content body with nice vertical flow
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFFE2E8F0.toInt() // slate-200 text
            textSize = 31f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val lines = content.split("\n")
        var yPos = 270f
        for (line in lines) {
            canvas.drawText(line, 60f, yPos, bodyPaint)
            yPos += 52f
        }

        return bitmap
    }

    private suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, name: String): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, name)
        try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        file.absolutePath
    }
}

/**
 * Custom Factory for instantiating the GalleryViewModel with its Repository.
 */
class GalleryViewModelFactory(private val repository: GalleryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
