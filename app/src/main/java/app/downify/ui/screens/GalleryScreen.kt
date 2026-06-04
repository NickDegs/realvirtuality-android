package app.downify.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.downify.R
import app.downify.data.ApiService
import app.downify.data.DownloadHistoryItem
import app.downify.ui.AuthViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<DownloadHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<DownloadHistoryItem?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching { apiService.getDownloadHistory() }
            .onSuccess { items = it }
            .onFailure { if (it is app.downify.data.ApiException.Unauthorized) authViewModel.logout() }
        isLoading = false
    }

    if (selectedItem != null) {
        GalleryDetailSheet(
            item = selectedItem!!,
            onDismiss = { selectedItem = null }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Galeri") }) }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Henüz indirme yok", style = MaterialTheme.typography.titleMedium)
                    Text("İndirdiğiniz içerikler burada görünecek", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    GalleryItemCard(item = item, onClick = { selectedItem = item })
                }
            }
        }
    }
}

@Composable
private fun GalleryItemCard(item: DownloadHistoryItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item.platform?.let { platform ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                    ) {
                        Text(platform, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(item.filename, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GalleryDetailSheet(item: DownloadHistoryItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.filename, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.fileSize?.let { size ->
                    Text("Boyut: ${String.format("%.1f MB", size / 1_000_000.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Platform: ${item.platform ?: "Bilinmiyor"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, item.downloadUrl.toUri()))
                onDismiss()
            }) { Text(stringResource(R.string.share_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
