package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GalleryDatabase
import com.example.data.GalleryRepository
import com.example.ui.GalleryScreen
import com.example.ui.GalleryViewModel
import com.example.ui.GalleryViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the local storage database and Repository layer
        val database = GalleryDatabase.getDatabase(applicationContext)
        val repository = GalleryRepository(database.galleryDao())
        
        setContent {
            MyApplicationTheme {
                // Instantiate the ViewModel using our custom VM Factory
                val viewModel: GalleryViewModel = viewModel(
                    factory = GalleryViewModelFactory(repository)
                )
                
                // Show our beautiful text-recognition gallery
                GalleryScreen(viewModel = viewModel)
            }
        }
    }
}
