package app.realvirtuality.ui.screens

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import app.realvirtuality.data.ApiException
import app.realvirtuality.data.ApiService
import app.realvirtuality.data.ScheduledDownload
import app.realvirtuality.ui.AuthViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ScheduledDownloadScreen(
    apiService: ApiService,
    authViewModel: AuthViewModel
) {
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<ScheduledDownload>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadItems() {
        scope.launch {
            isLoading = true
            try {
                items = apiService.getScheduledDownloads()
            } catch (e: ApiException.Unauthorized) {
                authViewModel.logout()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Bağlantı hatası"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadItems() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                isLoading && items.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                items.isEmpty() -> {
                    EmptyScheduledState(Modifier.align(Alignment.Center)) { showAddDialog = true }
                }
                else -> {
                    LazyColumn {
                        items(items) { item ->
                            ScheduledItemRow(
                                item = item,
                                onDelete = {
                                    scope.launch {
                                        try {
                                            apiService.deleteScheduledDownload(item.id)
                                            items = items.filter { it.id != item.id }
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduledDialog(
            apiService = apiService,
            onDismiss = { showAddDialog = false },
            onAdded = { showAddDialog = false; loadItems() }
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Hata") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("Tamam") } }
        )
    }
}

@Composable
private fun EmptyScheduledState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Planlı İndirme Yok", style = MaterialTheme.typography.titleLarge)
        Text(
            "Belirli bir saatte otomatik indirilmesini istediğin videoları ekle",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(onClick = onAdd) { Text("İndirme Planla") }
    }
}

@Composable
private fun ScheduledItemRow(item: ScheduledDownload, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(item.status)
                Text(formatScheduledDate(item.scheduledAt), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item.title?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            Text(item.url, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(
                if (item.quality == "best") "En İyi Kalite" else "${item.quality}p",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (color, label) = when (status) {
        "pending" -> MaterialTheme.colorScheme.tertiary to "Bekliyor"
        "completed" -> MaterialTheme.colorScheme.primary to "Tamamlandı"
        "failed" -> MaterialTheme.colorScheme.error to "Başarısız"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to status
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun AddScheduledDialog(
    apiService: ApiService,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var urlText by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf("best") }
    var scheduledHoursFromNow by remember { mutableFloatStateOf(1f) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("İndirme Planla") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Video URL'si") },
                    placeholder = { Text("Instagram, TikTok, YouTube...") },
                    trailingIcon = {
                        IconButton(onClick = { urlText = clipboard.getText()?.text ?: "" }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Yapıştır")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Text("Kalite", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("best" to "En İyi", "1080" to "1080p", "720" to "720p").forEach { (q, label) ->
                            FilterChip(
                                selected = quality == q,
                                onClick = { quality = q },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Column {
                    Text(
                        "Yaklaşık ${scheduledHoursFromNow.toInt()} saat sonra",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = scheduledHoursFromNow,
                        onValueChange = { scheduledHoursFromNow = it },
                        valueRange = 1f..72f,
                        steps = 70
                    )
                    Text(
                        "1 saat ile 3 gün arasında",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val scheduledAt = Instant.now()
                                .plusSeconds((scheduledHoursFromNow * 3600).toLong())
                                .toString()
                            apiService.scheduleDownload(
                                url = urlText,
                                scheduledAt = scheduledAt,
                                quality = quality
                            )
                            onAdded()
                        } catch (e: ApiException.ServerError) {
                            errorMessage = e.msg
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Bağlantı hatası"
                        }
                        isSaving = false
                    }
                },
                enabled = urlText.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Planla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

private fun formatScheduledDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (_: Exception) { iso }
}
