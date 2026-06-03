package app.realvirtuality.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.realvirtuality.data.ApiService
import app.realvirtuality.data.BulkDownloadListResponse
import app.realvirtuality.data.BulkItem
import app.realvirtuality.ui.AuthViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkDownloadScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    val scope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var bulkResponse by remember { mutableStateOf<BulkDownloadListResponse?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isDownloading by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Toplu İndirme") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when {
                isDone -> DoneState(onReset = { isDone = false; bulkResponse = null; urlText = "" })
                bulkResponse != null -> BulkItemList(
                    bulk = bulkResponse!!,
                    selectedIds = selectedIds,
                    isDownloading = isDownloading,
                    onToggle = { id ->
                        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                    },
                    onSelectAll = { selectedIds = if (selectedIds.size == bulkResponse!!.items.size) emptySet() else bulkResponse!!.items.map { it.id }.toSet() },
                    onDownload = {
                        scope.launch {
                            isDownloading = true
                            runCatching {
                                apiService.startBulkDownload(bulkResponse!!.bulkId, selectedIds.toList())
                            }.onSuccess { isDone = true }
                             .onFailure { errorMessage = it.localizedMessage }
                            isDownloading = false
                        }
                    }
                )
                else -> InputState(
                    urlText = urlText,
                    isFetching = isFetching,
                    onUrlChange = { urlText = it },
                    onFetch = {
                        scope.launch {
                            isFetching = true
                            runCatching { apiService.fetchBulkItems(urlText) }
                                .onSuccess {
                                    bulkResponse = it
                                    selectedIds = it.items.map { item -> item.id }.toSet()
                                }
                                .onFailure { errorMessage = it.localizedMessage }
                            isFetching = false
                        }
                    }
                )
            }
        }
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
private fun InputState(urlText: String, isFetching: Boolean, onUrlChange: (String) -> Unit, onFetch: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Filled.Layers, null, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary)
        Text("Profil veya Playlist URL'si", style = MaterialTheme.typography.titleMedium)
        Text("Instagram profil, YouTube playlist, TikTok kullanıcı sayfası URL'lerini destekler",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = urlText,
            onValueChange = onUrlChange,
            placeholder = { Text("instagram.com/kullanici veya youtube.com/playlist...") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onFetch,
            enabled = urlText.isNotBlank() && !isFetching,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isFetching) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("İçerikleri Listele")
        }
    }
}

@Composable
private fun BulkItemList(
    bulk: BulkDownloadListResponse,
    selectedIds: Set<String>,
    isDownloading: Boolean,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDownload: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${bulk.total} içerik", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            TextButton(onClick = onSelectAll) {
                Text(if (selectedIds.size == bulk.items.size) "Seçimi Kaldır" else "Tümünü Seç")
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(bulk.items) { item ->
                BulkItemRow(item = item, isSelected = item.id in selectedIds, onToggle = { onToggle(item.id) })
                HorizontalDivider()
            }
        }

        Surface(shadowElevation = 8.dp) {
            Button(
                onClick = onDownload,
                enabled = selectedIds.isNotEmpty() && !isDownloading,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
            ) {
                if (isDownloading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("${selectedIds.size} İçeriği İndir")
            }
        }
    }
}

@Composable
private fun BulkItemRow(item: BulkItem, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.thumbnail != null) {
            AsyncImage(model = item.thumbnail, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp, 40.dp))
        } else {
            Box(Modifier.size(60.dp, 40.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Layers, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(item.title ?: item.url, style = MaterialTheme.typography.bodySmall, maxLines = 2,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DoneState(onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("İndirmeler Başlatıldı!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Seçilen içerikler arka planda indiriliyor.\nTamamlananlar galeride görünecek.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onReset) { Text("Yeni İndirme") }
    }
}
