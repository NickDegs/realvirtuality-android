package app.mediafy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.mediafy.data.ApiService
import app.mediafy.data.AutoSubscription
import app.mediafy.ui.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDownloadScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    val scope = rememberCoroutineScope()
    var subscriptions by remember { mutableStateOf<List<AutoSubscription>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching { apiService.getAutoSubscriptions() }
            .onSuccess { subscriptions = it }
            .onFailure { if (it is app.mediafy.data.ApiException.Unauthorized) authViewModel.logout() }
        isLoading = false
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Otomatik İndirme") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ekle")
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            subscriptions.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Otomatik indirme yok", style = MaterialTheme.typography.titleMedium)
                    Text("+ butonuna tıklayarak hesap ekleyin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(subscriptions, key = { it.id }) { sub ->
                    AutoSubscriptionItem(
                        subscription = sub,
                        onDelete = {
                            scope.launch {
                                runCatching { apiService.deleteAutoSubscription(sub.id) }
                                    .onSuccess { subscriptions = subscriptions.filter { it.id != sub.id } }
                                    .onFailure { errorMessage = it.localizedMessage }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { url, frequency ->
                scope.launch {
                    runCatching { apiService.addAutoSubscription(url, frequency) }
                        .onSuccess { subscriptions = subscriptions + it }
                        .onFailure { errorMessage = it.localizedMessage }
                }
                showAddDialog = false
            }
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
private fun AutoSubscriptionItem(subscription: AutoSubscription, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(subscription.title ?: subscription.url, maxLines = 1) },
        supportingContent = {
            Column {
                Text(subscription.url, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${frequencyLabel(subscription.frequency)} • ${subscription.downloadCount} indirme",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                Icons.Filled.Schedule,
                null,
                tint = if (subscription.active) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun AddSubscriptionDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var urlText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("daily") }
    val frequencies = listOf("hourly" to "Saatte bir", "daily" to "Günlük", "weekly" to "Haftalık")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hesap Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Profil URL'si") },
                    placeholder = { Text("instagram.com/hesap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Kontrol Sıklığı", style = MaterialTheme.typography.labelMedium)
                frequencies.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = frequency == value, onClick = { frequency = value })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(urlText, frequency) }, enabled = urlText.isNotBlank()) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

private fun frequencyLabel(f: String) = when (f) {
    "hourly" -> "Saatte bir"
    "daily" -> "Günlük"
    "weekly" -> "Haftalık"
    else -> f
}
