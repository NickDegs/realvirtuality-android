package app.mediafy.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.mediafy.R
import app.mediafy.data.ApiException
import app.mediafy.data.ApiService
import app.mediafy.data.DownloadResponse
import app.mediafy.ui.AuthViewModel
import app.mediafy.ui.components.DownloadProgressCard
import kotlinx.coroutines.launch

enum class DownloadMode(val label: String, val icon: @Composable () -> Unit) {
    SINGLE("Tek Video", { Icon(Icons.Default.Download, null) }),
    CLIP("Klip", { Icon(Icons.Default.ContentCut, null) }),
    GIF("GIF", { Icon(Icons.Default.PlayArrow, null) }),
    SUBTITLES("Altyazı", { Icon(Icons.Default.Subtitles, null) }),
    KEY_MOMENTS("Önemli Anlar", { Icon(Icons.Default.AutoAwesome, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(DownloadMode.SINGLE) }

    if (showSettings) {
        SettingsScreen(
            authViewModel = authViewModel,
            apiService = apiService,
            onDismiss = { showSettings = false }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Mode selector
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode.label) },
                        leadingIcon = if (selectedMode == mode) mode.icon else null
                    )
                }
            }

            HorizontalDivider()

            // Content for selected mode
            Box(Modifier.fillMaxSize()) {
                when (selectedMode) {
                    DownloadMode.SINGLE -> SingleDownloadContent(
                        authViewModel = authViewModel,
                        apiService = apiService
                    )
                    DownloadMode.CLIP -> ClipScreen(
                        apiService = apiService,
                        authViewModel = authViewModel,
                        isGifMode = false
                    )
                    DownloadMode.GIF -> ClipScreen(
                        apiService = apiService,
                        authViewModel = authViewModel,
                        isGifMode = true
                    )
                    DownloadMode.SUBTITLES -> SubtitleScreen(
                        apiService = apiService,
                        authViewModel = authViewModel
                    )
                    DownloadMode.KEY_MOMENTS -> KeyMomentsScreen(
                        apiService = apiService,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleDownloadContent(
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urlText by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadTask by remember { mutableStateOf<DownloadResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var removeWatermark by remember { mutableStateOf(true) }
    var audioOnly by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf("best") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.media_url), style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                urlText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            }) {
                Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.paste_url))
            }
        }

        // Options row
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = removeWatermark,
                onClick = { removeWatermark = !removeWatermark },
                label = { Text("Watermark'sız") },
                leadingIcon = if (removeWatermark) {
                    { Icon(Icons.Default.VerifiedUser, null, Modifier.size(16.dp)) }
                } else null
            )
            FilterChip(
                selected = audioOnly,
                onClick = { audioOnly = !audioOnly },
                label = { Text(if (audioOnly) "MP3" else "Video") },
                leadingIcon = if (audioOnly) {
                    { Icon(Icons.Default.MusicNote, null, Modifier.size(16.dp)) }
                } else null
            )
            listOf("best" to "En İyi", "1080" to "1080p", "720" to "720p", "480" to "480p").forEach { (q, label) ->
                FilterChip(
                    selected = quality == q,
                    onClick = { quality = q },
                    label = { Text(label) }
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    isDownloading = true
                    errorMessage = null
                    try {
                        downloadTask = apiService.startDownload(
                            url = urlText,
                            quality = quality,
                            audioOnly = audioOnly,
                            noWatermark = removeWatermark
                        )
                    } catch (e: ApiException.Unauthorized) {
                        authViewModel.logout()
                    } catch (e: ApiException.ServerError) {
                        errorMessage = e.msg
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Bağlantı hatası"
                    }
                    isDownloading = false
                }
            },
            enabled = urlText.isNotBlank() && !isDownloading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("İndiriliyor...")
            } else {
                Icon(Icons.Filled.Download, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.download))
            }
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        downloadTask?.let { task ->
            DownloadProgressCard(
                taskId = task.taskId,
                apiService = apiService,
                onDismiss = { downloadTask = null; urlText = "" }
            )
        }

        Text(
            stringResource(R.string.supported_platforms),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Instagram", "TikTok", "YouTube", "Twitter/X", "Facebook", "Reddit", "Twitch", "1000+")
                .forEach { platform ->
                    SuggestionChip(onClick = {}, label = { Text(platform) })
                }
        }
    }
}
