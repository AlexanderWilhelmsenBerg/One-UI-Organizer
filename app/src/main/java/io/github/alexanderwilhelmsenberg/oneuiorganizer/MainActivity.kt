package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidAppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidInstalledAppSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.PlatformAppsSpikeScreen
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme

class MainActivity : ComponentActivity() {
    private val scanGeneration = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneUiOrganizerTheme {
                val installedAppSource =
                    remember {
                        AndroidInstalledAppSource(applicationContext)
                    }
                val appLauncher =
                    remember {
                        AndroidAppLauncher(applicationContext)
                    }

                PlatformAppsSpikeScreen(
                    installedAppSource = installedAppSource,
                    appLauncher = appLauncher,
                    scanGeneration = scanGeneration.intValue,
                    onDismiss = ::finish,
                    onLaunchSucceeded = ::finish
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scanGeneration.intValue += 1
    }
}
