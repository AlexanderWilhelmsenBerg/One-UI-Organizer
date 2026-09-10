package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.R
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import kotlinx.coroutines.CancellationException

@Composable
internal fun PlatformAppsSpikeScreen(
    installedAppSource: InstalledAppSource,
    appLauncher: AppLauncher,
    scanGeneration: Int,
    onDismiss: () -> Unit,
    onLaunchSucceeded: () -> Unit
) {
    var state by remember { mutableStateOf<PlatformAppsSpikeState>(PlatformAppsSpikeState.Loading) }

    LaunchedEffect(installedAppSource, scanGeneration) {
        if (scanGeneration <= 0) {
            return@LaunchedEffect
        }

        state = PlatformAppsSpikeState.Loading
        state =
            try {
                PlatformAppsSpikeState.Ready(installedAppSource.loadInstalledApps())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                PlatformAppsSpikeState.Error
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.platform_spike_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.platform_spike_dismiss))
                    }
                }

                Text(
                    text = stringResource(R.string.platform_spike_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                when (val currentState = state) {
                    PlatformAppsSpikeState.Loading -> {
                        Text(text = stringResource(R.string.platform_spike_loading))
                    }

                    PlatformAppsSpikeState.Error -> {
                        Text(text = stringResource(R.string.platform_spike_error))
                    }

                    is PlatformAppsSpikeState.Ready -> {
                        val packageCount = currentState.apps.distinctBy(InstalledApp::id).size
                        val multiTargetPackages =
                            currentState.apps
                                .groupBy { app -> app.id }
                                .filterValues { apps -> apps.size > 1 }
                                .entries
                                .sortedBy { entry -> entry.key.packageName.lowercase() }

                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.platform_spike_count,
                                    currentState.apps.size,
                                    currentState.apps.size
                                )
                        )
                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.platform_spike_package_count,
                                    packageCount,
                                    packageCount
                                )
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Text(
                                    text =
                                        pluralStringResource(
                                            R.plurals.platform_spike_multi_target_package_count,
                                            multiTargetPackages.size,
                                            multiTargetPackages.size
                                        ),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            items(
                                items = multiTargetPackages,
                                key = { entry -> "multiple/${entry.key.packageName}" }
                            ) { entry ->
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = entry.key.packageName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    entry.value.forEach { app ->
                                        Text(
                                            text = "${app.label}: ${app.launchTargetId.className}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            item {
                                Text(
                                    modifier = Modifier.padding(top = 8.dp),
                                    text = stringResource(R.string.platform_spike_all_targets),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            items(
                                items = currentState.apps,
                                key = { app ->
                                    "${app.launchTargetId.packageName}/${app.launchTargetId.className}"
                                }
                            ) { app ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (appLauncher.launch(app.launchTargetId)) {
                                            onLaunchSucceeded()
                                        }
                                    }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(text = app.label)
                                        Text(
                                            text =
                                                "${app.launchTargetId.packageName}/" +
                                                    app.launchTargetId.className,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface PlatformAppsSpikeState {
    data object Loading : PlatformAppsSpikeState

    data object Error : PlatformAppsSpikeState

    data class Ready(val apps: List<InstalledApp>) : PlatformAppsSpikeState
}
