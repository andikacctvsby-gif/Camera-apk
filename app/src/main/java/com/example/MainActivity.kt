package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.CameraAiViewModel
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.ShareScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CameraAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val context = LocalContext.current
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    val savedPhotos by viewModel.savedPhotos.collectAsStateWithLifecycle()

                    // Toast feedback
                    LaunchedEffect(state.toastMessage) {
                        state.toastMessage?.let { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }

                    when (state.currentScreen) {
                        AppScreen.CAMERA -> {
                            CameraScreen(
                                viewModel = viewModel,
                                savedPhotosCount = savedPhotos.size
                            )
                        }
                        AppScreen.EDITOR -> {
                            EditorScreen(
                                viewModel = viewModel,
                                state = state
                            )
                        }
                        AppScreen.SHARE -> {
                            ShareScreen(
                                viewModel = viewModel,
                                state = state
                            )
                        }
                        AppScreen.GALLERY -> {
                            GalleryScreen(
                                viewModel = viewModel,
                                photos = savedPhotos
                            )
                        }
                    }
                }
            }
        }
    }
}
