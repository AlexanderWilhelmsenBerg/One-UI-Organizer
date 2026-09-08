package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.oneUiOrganizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            oneUiOrganizerTheme {
                Surface {
                    Text(text = stringResource(R.string.scaffold_ready))
                }
            }
        }
    }
}
