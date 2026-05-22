package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.data.GalleryImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(viewModel: GalleryViewModel) {
    val context = LocalContext.current

    // Trigger sample data injection inside a LaunchedEffect at birth
    LaunchedEffect(Unit) {
        viewModel.prepopulateDatabaseIfEmpty(context)
    }

    // Collect Viewmodel State flows
    val images by viewModel.galleryImages.collectAsState()
    val albums by viewModel.allAlbums.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val activeImage by viewModel.activeImageForDetail.collectAsState()
    val ocrResultText by viewModel.ocrResultText.collectAsState()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()
    val isImageGeneratorOpen by viewModel.createImageSheetOpen.collectAsState()

    // Frosted colors palette mapping user's HTML spec
    val backgroundColor = Color(0xFFFEF7FF)
    val frostedSurface = Color(0xFFF3EDF7)
    val activePillColor = Color(0xFFE8DEF8)
    val textPrimary = Color(0xFF1D192B)
    val textSecondary = Color(0xFF49454F)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        bottomBar = {
            // High fidelity Custom Navigation Bar designed mirroring requested HTML specification
            Column {
                HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(frostedSurface.copy(alpha = 0.95f))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item 1: Photos (Active style)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectAlbum(null) }
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(activePillColor)
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Photos",
                                tint = textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Photos",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Item 2: Search Status (Indicator only)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Info",
                            tint = textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Search",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Item 3: Folders (Active/Album indicator)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folders Info",
                            tint = textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Library",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Item 4: Settings (Static info representation)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Settings",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Elegant FAB for custom scannable image creation
            FloatingActionButton(
                onClick = { viewModel.setCreateImageSheetOpen(true) },
                containerColor = activePillColor,
                contentColor = textPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = "Scan Text Creator",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Sleek Frosted Capsule Search Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(frostedSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search \"meeting notes\" or \"receipts\"...",
                                    color = textSecondary.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextFieldWithHint(
                                query = searchQuery,
                                onUpdate = { viewModel.setSearchQuery(it) },
                                textColor = textPrimary
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E7FF))
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                text = "JD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA)
                            )
                        }
                    }
                }

                // 2. Quick Album / Filter Tags Layout
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterTag(
                            label = "All Photos",
                            isSelected = selectedAlbum == null,
                            onClick = { viewModel.selectAlbum(null) }
                        )
                    }
                    val predefinedFilters = listOf("Credentials", "Documents", "Receipts", "Screenshots")
                    items(predefinedFilters) { filterName ->
                        val isSelected = selectedAlbum?.equals(filterName, ignoreCase = true) == true
                        FilterTag(
                            label = filterName,
                            isSelected = isSelected,
                            onClick = { viewModel.selectAlbum(filterName) }
                        )
                    }
                }

                // 3. Albums Visual Horizontal Panel (Maintaining current structures)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Albums Collection",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "View all",
                            fontSize = 12.sp,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.selectAlbum(null) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Recents / All Album Card
                        AlbumItemCard(
                            title = "Recent Scans",
                            countLabel = "${images.size} items",
                            isSelected = selectedAlbum == null,
                            onClick = { viewModel.selectAlbum(null) },
                            modifier = Modifier.weight(1f),
                            gradStart = Color(0xFF6366F1),
                            gradEnd = Color(0xFF312E81)
                        )

                        // Scanned Text Category Album Card
                        AlbumItemCard(
                            title = "Scanned Text",
                            countLabel = "Document files",
                            isSelected = selectedAlbum?.equals("Documents", ignoreCase = true) == true,
                            onClick = { viewModel.selectAlbum("Documents") },
                            modifier = Modifier.weight(1f),
                            gradStart = Color(0xFFA855F7),
                            gradEnd = Color(0xFF581C87),
                            showDocIcon = true
                        )

                        // Screenshots Album Card
                        AlbumItemCard(
                            title = "Screenshots",
                            countLabel = "Credentials",
                            isSelected = selectedAlbum?.equals("Screenshots", ignoreCase = true) == true,
                            onClick = { viewModel.selectAlbum("Screenshots") },
                            modifier = Modifier.weight(1f),
                            gradStart = Color(0xFF06B6D4),
                            gradEnd = Color(0xFF164E63)
                        )
                    }
                }

                // Heading for Selected Feed
                Text(
                    text = if (selectedAlbum != null) "Album: ${selectedAlbum}" else "All Captured OCR Media",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                )

                // 4. Main Gallery Grid (3 Columns)
                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterFrames,
                                contentDescription = "Empty Folder",
                                tint = textSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching text images found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the scanner FAB at bottom right to instantly write text and generate custom photos!",
                                fontSize = 12.sp,
                                color = textSecondary.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    // We render a responsive, elegant Grid flow.
                    // Because simple LazyVerticalGrid inside a verticalScroll is problematic,
                    // we calculate chunks of 3 row-by-row or allocate direct height nicely.
                    // To maintain ultimate scroll flexibility, let's chunk files:
                    val chunks = images.chunked(3)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chunks.forEach { rowImages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowImages.forEach { image ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(
                                                width = if (image.ocrText != null) 1.5.dp else 0.dp,
                                                color = if (image.ocrText != null) Color(0xFFC084FC) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.viewImageDetail(image) }
                                    ) {
                                        // Dynamic Coil load from local filesystem uri
                                        Image(
                                            painter = rememberAsyncImagePainter(File(image.filePath)),
                                            contentDescription = image.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Linear overlay shroud
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.35f)
                                                        ),
                                                        startY = 140f
                                                    )
                                                )
                                        )

                                        // Badge check or text indicators at top right (OCR active)
                                        if (image.ocrText != null) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.White.copy(alpha = 0.9f))
                                                    .border(0.5.dp, Color(0xFF818CF8), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.TextFields,
                                                        contentDescription = "OCR Scanned",
                                                        tint = Color(0xFF4F46E5),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "OCR",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF4F46E5)
                                                    )
                                                }
                                            }
                                        }

                                        // Display compact label on bottom
                                        Text(
                                            text = image.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // Pad row out if it's not a block of 3 elements
                                if (rowImages.size < 3) {
                                    val blanksCount = 3 - rowImages.size
                                    for (b in 0 until blanksCount) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Dialog Overlays managed by viewmodel flow state ---

            // Detail Panel: View Image content, view scanning options, copy texts, etc.
            if (activeImage != null) {
                ImageDetailDialog(
                    image = activeImage!!,
                    extractedText = ocrResultText,
                    isProcessing = isOcrProcessing,
                    onDismiss = { viewModel.viewImageDetail(null) },
                    onReScan = { viewModel.runOcrOnImage(activeImage!!) },
                    onDelete = {
                        viewModel.deleteImage(activeImage!!)
                        viewModel.viewImageDetail(null)
                    }
                )
            }

            // Image creation popup: User can design an image with code/receipt/creds/text templates and push to DB
            if (isImageGeneratorOpen) {
                ImageCreatorDialog(
                    onDismiss = { viewModel.setCreateImageSheetOpen(false) },
                    onCreateImage = { title, preset, album, text ->
                        viewModel.insertCustomImage(context, title, preset, album, text)
                        viewModel.setCreateImageSheetOpen(false)
                        Toast.makeText(context, "New custom text-image compiled & loaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

/**
 * Filter Chip/Tag rendering
 */
@Composable
fun FilterTag(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFE8DEF8) else Color.White)
            .border(
                1.dp,
                if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color(0xFF1D192B) else Color(0xFF49454F)
        )
    }
}

/**
 * High fidelity rounded Album card
 */
@Composable
fun AlbumItemCard(
    title: String,
    countLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    gradStart: Color,
    gradEnd: Color,
    showDocIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradStart, gradEnd)
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) Color(0xFFC084FC) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        // Doc icon indicator top right
        if (showDocIcon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Document Category",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = countLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(0.7f)
            )
        }
    }
}

/**
 * Custom text field wrapped with clear composable styling
 */
@Composable
fun BasicTextFieldWithHint(
    query: String,
    onUpdate: (String) -> Unit,
    textColor: Color
) {
    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onUpdate,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

/**
 * Full Detailed Modal panel for inspection and extraction of specific values
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageDetailDialog(
    image: GalleryImage,
    extractedText: String?,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onReScan: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val fmtdDate = remember(image.timestamp) {
        SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(image.timestamp))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFEF7FF)) // Frosted tone
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF1D192B)
                            )
                        }
                        Text(
                            text = image.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Image",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Media File Frame Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF3EDF7))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(File(image.filePath)),
                            contentDescription = "Expanded View",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mini Metadata Pill Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BadgePill(title = "Album: ${image.album}", color = Color(0xFFE8DEF8))
                        BadgePill(title = fmtdDate, color = Color(0xFFF3EDF7))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detected Objects Section
                    val objectLabels = remember(image.detectedObjectsLabel) {
                        image.detectedObjectsLabel?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    }

                    if (objectLabels.isNotEmpty()) {
                        Text(
                            text = "Detected Objects (Offline)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            objectLabels.forEach { label ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE0F2FE))
                                        .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = Color(0xFF0369A1),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0369A1)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Detected Objects (Offline)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isProcessing) "Analyzing objects offline..." else "No tags identified yet. Tap Scan to analyze.",
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280).copy(0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // OCR Output Header Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extracted Live Text",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Copy Text Clipboard Utility
                            if (extractedText != null && extractedText.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Extracted text", extractedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied extracted text to Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xFF1D192B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 10.sp, color = Color(0xFF1D192B), fontWeight = FontWeight.Bold)
                                }
                            }

                            // Request update/re-scan action
                            Button(
                                onClick = onReScan,
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Scan",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // OCR Raw text payload rendering
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .heightIn(min = 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        if (isProcessing) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Analyzing and scanning bitmap elements...", fontSize = 12.sp, color = Color(0xFF49454F))
                            }
                        } else if (extractedText == null) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Text not recognized yet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF49454F)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap Scan to extract digital strings locally.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF49454F).copy(0.6f)
                                )
                            }
                        } else if (extractedText.isBlank()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No written textual components identified in this scan.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF49454F),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = extractedText,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1D192B)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact pill label
 */
@Composable
fun BadgePill(title: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D192B)
        )
    }
}

/**
 * High fidelity custom scannable image creator panel inside active dialogues
 */
@Composable
fun ImageCreatorDialog(
    onDismiss: () -> Unit,
    onCreateImage: (title: String, templateType: String, albumName: String, text: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var userText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("creds") } // preset types: creds, code, receipt, agenda
    var selectedAlbum by remember { mutableStateOf("Credentials") }

    val options = listOf(
        Triple("creds", "🔑 Wi-Fi/Pws", "Credentials"),
        Triple("code", "💻 Snippets", "Screenshots"),
        Triple("receipt", "🧾 Receipts", "Receipts"),
        Triple("agenda", "📝 Notes", "Documents")
    )

    // Sync template default texts for speed of testing
    LaunchedEffect(selectedType) {
        if (userText.isEmpty()) {
            userText = when (selectedType) {
                "creds" -> "Home Network Router\nSSID: FiberGlass_5G\nPIN: star-violet-8833"
                "code" -> "fun fetchOcrText() {\n  val result = parser.scan(bitmap)\n  Log.d(\"OCR\", result)\n}"
                "receipt" -> "BOOTH COFFEE\n---------------------\nCaramel Frost   $5.80\nScone           $3.50\n---------------------\nTOTAL           $9.30"
                else -> "- Project Sync Agenda\n- Integrate ML Kit Locally\n- Polish custom views\n- Refactor memory state"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFEF7FF),
            border = BorderStroke(1.dp, Color.White.copy(0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Synthesize Scannable Image",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D192B)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Generate a beautifully styled high-contrast photo immediately with custom written text overlays. This runs ML Kit locally of your virtual flow.",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F).copy(alpha = 0.8f)
                )

                // Title Input
                Column {
                    Text("Image Label Title", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. My Private WiFi info") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Choose Template Styling Row
                Column {
                    Text("Render Template Style", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { triple ->
                            val isChosen = selectedType == triple.first
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) Color(0xFFE8DEF8) else Color(0xFFF3EDF7))
                                    .border(
                                        1.dp,
                                        if (isChosen) Color(0xFF4F46E5) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedType = triple.first
                                        selectedAlbum = triple.third
                                        // Reset default template string mapping correctly
                                        userText = ""
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = triple.second,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D192B)
                                )
                            }
                        }
                    }
                }

                // Album Destination Display
                Column {
                    Text("Auto Sort Album Destination", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedAlbum,
                        onValueChange = { selectedAlbum = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Body Text Input (Multi line)
                Column {
                    Text("Custom Written Content (Visible in Photo & Scannable)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = userText,
                        onValueChange = { userText = it },
                        placeholder = { Text("Type custom words patterns to scan locally...") },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Submit Button
                Button(
                    onClick = {
                        val finalTitle = title.trim().ifBlank { "Unlabeled Capture" }
                        val finalAlbum = selectedAlbum.trim().ifBlank { "Scanned Text" }
                        onCreateImage(finalTitle, selectedType, finalAlbum, userText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Compile, Load & Extract Text", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// State flows helper
