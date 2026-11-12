package com.glyphplay.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glyphplay.player.ui.GlyphPlayApp
import com.glyphplay.player.ui.PlayerViewModel
import com.glyphplay.player.ui.theme.GlyphPlayTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val audioGranted = if (Build.VERSION.SDK_INT >= 33) {
            result[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            result[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        viewModel.onPermission(audioGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        askPermissions()
        setContent {
            GlyphPlayTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                GlyphPlayApp(
                    state = state,
                    onPlay = viewModel::play,
                    onToggle = viewModel::togglePlay,
                    onNext = viewModel::next,
                    onPrev = viewModel::previous,
                    onSeek = viewModel::seek,
                    onShuffle = viewModel::toggleShuffle,
                    onRepeat = viewModel::cycleRepeat,
                    onSleep = viewModel::setSleepTimer,
                    onTab = viewModel::openTab,
                    onRequestPermission = { askPermissions() },
                )
            }
        }
    }

    private fun askPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
        permissionLauncher.launch(needed)
    }
}
