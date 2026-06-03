package app.mediafy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.mediafy.data.ApiException
import app.mediafy.data.ApiService
import app.mediafy.data.DownloadResponse
import app.mediafy.data.SubtitleTrack
import app.mediafy.ui.AuthViewModel
import app.mediafy.ui.components.DownloadProgressCard
import kotlinx.coroutines.launch

@Composable
fun SubtitleScreen(
    apiService: ApiService,
    authViewModel: AuthViewModel
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var urlText by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf<List<SubtitleTrack>>(emptyList()) }
    var selectedLanguage by remember { mutableStateOf("") }
    var embedInVideo by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadTask by remember { mutableStateOf<DownloadResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp).align(Alignment.CenterVertically)
                )
                Column {
                    Text("Altyazı İndirme", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Video ile birlikte altyazı dosyasını al",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // URL input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Video URL'si", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = urlText,
                onValueChange = {
                    urlText = it
                    tracks = emptyList()
                    selectedLanguage = ""
                },
                placeholder = { Text("YouTube, TikTok, Instagram...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = {
                    IconButton(onClick = {
                        urlText = clipboard.getText()?.text ?: ""
                        tracks = emptyList()
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Yapıştır")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (tracks.isEmpty() && urlText.isNotBlank()) {
                Button(
                    onClick = {
                        scope.launch {
                            isFetching = true
                            errorMessage = null
                            try {
                                tracks = apiService.getSubtitleTracks(urlText)
                                if (tracks.isNotEmpty()) selectedLanguage = tracks.first().language
                            } catch (e: ApiException.Unauthorized) {
                                authViewModel.logout()
                            } catch (e: ApiException.ServerError) {
                                errorMessage = e.msg
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Bağlantı hatası"
                            }
                            isFetching = false
                        }
                    },
                    enabled = !isFetching,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isFetching) "Altyazılar aranıyor..." else "Altyazıları Getir")
                }
            }
        }

        // Fetching indicator
        if (isFetching) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Altyazılar aranıyor...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Track selection
        if (tracks.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${tracks.size} altyazı dili bulundu",
                        style = MaterialTheme.typography.titleSmall
                    )
                    tracks.forEach { track ->
                        val isSelected = selectedLanguage == track.language
                        Card(
                            onClick = { selectedLanguage = track.language },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(track.languageName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${track.language.uppercase()} • ${track.format.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Embed toggle
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Videoya Göm", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (embedInVideo) "Altyazı videoyla birleştirilir (tek dosya)"
                            else "Ayrı .srt dosyası olarak indirilir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = embedInVideo, onCheckedChange = { embedInVideo = it })
                }
            }

            // Download button
            Button(
                onClick = {
                    scope.launch {
                        isDownloading = true
                        errorMessage = null
                        try {
                            downloadTask = apiService.startSubtitleDownload(
                                url = urlText,
                                language = selectedLanguage,
                                embed = embedInVideo
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
                enabled = selectedLanguage.isNotEmpty() && !isDownloading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    when {
                        isDownloading -> "İndiriliyor..."
                        embedInVideo -> "Video + Altyazı İndir"
                        else -> "Altyazı Dosyası İndir"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Progress card
        downloadTask?.let { task ->
            DownloadProgressCard(taskId = task.taskId, apiService = apiService) {
                downloadTask = null
            }
        }

        errorMessage?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
