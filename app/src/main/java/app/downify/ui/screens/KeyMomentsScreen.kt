package app.downify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.downify.data.ApiException
import app.downify.data.ApiService
import app.downify.data.VideoChapter
import app.downify.data.VideoInfo
import app.downify.ui.AuthViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun KeyMomentsScreen(
    apiService: ApiService,
    authViewModel: AuthViewModel
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var urlText by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isDownloading by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    var taskCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    when {
        isDone -> DoneView(taskCount = taskCount) {
            isDone = false; videoInfo = null; urlText = ""
        }
        videoInfo != null -> MomentsView(
            info = videoInfo!!,
            selectedIds = selectedIds,
            isDownloading = isDownloading,
            onToggle = { id ->
                selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
            },
            onSelectAll = { all ->
                selectedIds = if (all) videoInfo!!.chapters.map { it.id }.toSet() else emptySet()
            },
            onBack = { videoInfo = null; selectedIds = emptySet(); urlText = "" },
            onDownload = {
                scope.launch {
                    isDownloading = true
                    try {
                        val resp = apiService.startChapterDownload(urlText, selectedIds.toList())
                        taskCount = resp.taskIds.size
                        isDone = true
                    } catch (e: ApiException.Unauthorized) {
                        authViewModel.logout()
                    } catch (e: ApiException.ServerError) {
                        errorMessage = e.msg
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Bağlantı hatası"
                    }
                    isDownloading = false
                }
            }
        )
        else -> InputView(
            urlText = urlText,
            isFetching = isFetching,
            onUrlChange = { urlText = it },
            onPaste = { urlText = clipboard.getText()?.text ?: "" },
            onFetch = {
                scope.launch {
                    isFetching = true
                    errorMessage = null
                    try {
                        val info = apiService.getVideoInfo(urlText)
                        videoInfo = info
                        selectedIds = info.chapters.map { it.id }.toSet()
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
            errorMessage = errorMessage
        )
    }
}

@Composable
private fun InputView(
    urlText: String,
    isFetching: Boolean,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onFetch: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Text("Önemli Anlar", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Video chapter'larını veya yapay zeka ile tespit edilen önemli sahneleri listele, istediğin kısımları indir",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        OutlinedTextField(
            value = urlText,
            onValueChange = onUrlChange,
            label = { Text("Video URL'si") },
            placeholder = { Text("YouTube, TikTok, Instagram...") },
            trailingIcon = {
                IconButton(onClick = onPaste) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Yapıştır")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = onFetch,
            enabled = urlText.isNotBlank() && !isFetching,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isFetching) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isFetching) "Analiz ediliyor..." else "Önemli Anları Bul")
        }

        // Info card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nasıl Çalışır?", style = MaterialTheme.typography.titleSmall)
                listOf(
                    "1" to "Videonun chapter (bölüm) bilgileri otomatik getirilir",
                    "2" to "Bölüm yoksa yapay zeka sahne değişimlerini tespit eder",
                    "3" to "İstediğin anları seç, ayrı ayrı klip olarak indir"
                ).forEach { (num, text) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$num.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
private fun MomentsView(
    info: VideoInfo,
    selectedIds: Set<String>,
    isDownloading: Boolean,
    onToggle: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onBack: () -> Unit,
    onDownload: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Video header
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                info.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp, 50.dp).clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(info.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (info.hasAiChapters) {
                            AssistChip(
                                onClick = {},
                                label = { Text("AI Tespiti", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp))
                                }
                            )
                        } else {
                            AssistChip(
                                onClick = {},
                                label = { Text("Orijinal Bölümler", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        Text("• ${formatDuration(info.duration)}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Selection bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${info.chapters.size} bölüm", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onSelectAll(selectedIds.size != info.chapters.size) }) {
                Text(if (selectedIds.size == info.chapters.size) "Seçimi Kaldır" else "Tümünü Seç")
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(info.chapters) { chapter ->
                ChapterRow(
                    chapter = chapter,
                    isSelected = selectedIds.contains(chapter.id),
                    onClick = { onToggle(chapter.id) }
                )
                HorizontalDivider()
            }
        }

        // Download bar
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }
            Button(
                onClick = onDownload,
                enabled = selectedIds.isNotEmpty() && !isDownloading,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (isDownloading) "İndiriliyor..." else "${selectedIds.size} Bölümü İndir",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ChapterRow(chapter: VideoChapter, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected)
                Icons.Default.CheckCircle
            else
                Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (chapter.thumbnailUrl != null) {
            AsyncImage(
                model = chapter.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(70.dp, 44.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier.size(70.dp, 44.dp).clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {}
                Text(formatTime(chapter.startTime), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(Modifier.weight(1f)) {
            Text(chapter.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(
                "${formatTime(chapter.startTime)} → ${formatTime(chapter.endTime)} • ${formatDurationShort(chapter.endTime - chapter.startTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoneView(taskCount: Int, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("$taskCount bölüm indirmeye başladı!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Klip olarak kaydediliyor.\nGaleride görünecekler.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = onReset) {
            Text("Yeni Video")
        }
    }
}

private fun formatTime(s: Double): String {
    val h = s.toInt() / 3600
    val m = (s.toInt() % 3600) / 60
    val sec = s.toInt() % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun formatDuration(s: Double): String {
    val h = s.toInt() / 3600
    val m = (s.toInt() % 3600) / 60
    val sec = s.toInt() % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun formatDurationShort(s: Double): String {
    return if (s < 60) "${s.toInt()}s" else "${(s / 60).toInt()}dk"
}
