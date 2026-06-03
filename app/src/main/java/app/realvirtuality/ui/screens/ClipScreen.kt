package app.realvirtuality.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.realvirtuality.data.ApiException
import app.realvirtuality.data.ApiService
import app.realvirtuality.data.DownloadResponse
import app.realvirtuality.ui.AuthViewModel
import app.realvirtuality.ui.components.DownloadProgressCard
import kotlinx.coroutines.launch

@Composable
fun ClipScreen(
    apiService: ApiService,
    authViewModel: AuthViewModel,
    isGifMode: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var urlText by remember { mutableStateOf("") }
    var startMinutes by remember { mutableIntStateOf(0) }
    var startSeconds by remember { mutableIntStateOf(0) }
    var endMinutes by remember { mutableIntStateOf(0) }
    var endSeconds by remember { mutableIntStateOf(30) }
    var asGif by remember { mutableStateOf(isGifMode) }
    var gifFps by remember { mutableFloatStateOf(15f) }
    var isLoading by remember { mutableStateOf(false) }
    var downloadTask by remember { mutableStateOf<DownloadResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val startTime = (startMinutes * 60 + startSeconds).toDouble()
    val endTime = (endMinutes * 60 + endSeconds).toDouble()
    val isValid = urlText.isNotBlank() && endTime > startTime

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
                    imageVector = if (asGif) androidx.compose.material.icons.Icons.Default.PlayArrow
                    else androidx.compose.material.icons.Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp).align(Alignment.CenterVertically)
                )
                Column {
                    Text(
                        if (asGif) "GIF Oluşturucu" else "Klip Kesici",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (asGif) "Seçilen kısımdan animasyonlu GIF üret"
                        else "Videonun istediğin bölümünü indir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // URL
        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Video URL'si") },
            placeholder = { Text("Instagram, TikTok, YouTube...") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            trailingIcon = {
                IconButton(onClick = { urlText = clipboard.getText()?.text ?: "" }) {
                    Icon(androidx.compose.material.icons.Icons.Default.ContentPaste, contentDescription = "Yapıştır")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Time Range
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Zaman Aralığı", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeInput(
                        label = "Başlangıç",
                        minutes = startMinutes,
                        seconds = startSeconds,
                        onMinutesChange = { startMinutes = it },
                        onSecondsChange = { startSeconds = it },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        androidx.compose.material.icons.Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TimeInput(
                        label = "Bitiş",
                        minutes = endMinutes,
                        seconds = endSeconds,
                        onMinutesChange = { endMinutes = it },
                        onSecondsChange = { endSeconds = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (endTime <= startTime) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Bitiş zamanı başlangıçtan büyük olmalı",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    val dur = (endTime - startTime).toInt()
                    Text(
                        "Süre: ${dur}s (${String.format("%.1f", dur / 60.0)} dk)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // GIF Toggle
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GIF olarak oluştur", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = asGif, onCheckedChange = { asGif = it })
            }
        }

        // GIF Options
        if (asGif) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GIF Ayarları", style = MaterialTheme.typography.titleSmall)
                    Text("FPS: ${gifFps.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = gifFps,
                        onValueChange = { gifFps = it },
                        valueRange = 5f..24f,
                        steps = 18
                    )
                    Text(
                        "Düşük FPS → küçük dosya, yüksek FPS → akıcı animasyon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Download Button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        downloadTask = apiService.startClip(
                            url = urlText,
                            startTime = startTime,
                            endTime = endTime,
                            asGif = asGif,
                            quality = null
                        )
                    } catch (e: ApiException.Unauthorized) {
                        authViewModel.logout()
                    } catch (e: ApiException.ServerError) {
                        errorMessage = e.msg
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Bağlantı hatası"
                    }
                    isLoading = false
                }
            },
            enabled = isValid && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                when {
                    isLoading -> "Hazırlanıyor..."
                    asGif -> "GIF Oluştur"
                    else -> "Klibi İndir"
                },
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Progress
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

@Composable
private fun TimeInput(
    label: String,
    minutes: Int,
    seconds: Int,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = String.format("%02d", minutes),
                onValueChange = { v -> v.toIntOrNull()?.coerceIn(0, 59)?.let(onMinutesChange) },
                modifier = Modifier.width(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Text(":", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = String.format("%02d", seconds),
                onValueChange = { v -> v.toIntOrNull()?.coerceIn(0, 59)?.let(onSecondsChange) },
                modifier = Modifier.width(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}
