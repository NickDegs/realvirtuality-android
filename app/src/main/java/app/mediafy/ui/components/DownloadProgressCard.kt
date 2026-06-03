package app.mediafy.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.mediafy.R
import app.mediafy.data.ApiService
import app.mediafy.data.DownloadStatus
import kotlinx.coroutines.delay

@Composable
fun DownloadProgressCard(
    taskId: String,
    apiService: ApiService,
    onDismiss: () -> Unit
) {
    var status by remember { mutableStateOf<DownloadStatus?>(null) }
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        while (true) {
            runCatching { apiService.getDownloadStatus(taskId) }
                .onSuccess { status = it }
            val s = status?.status
            if (s == "completed" || s == "failed") break
            delay(2_000)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val s = status
            when {
                s == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.preparing))
                }

                s.status == "completed" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF22C55E))
                        Text(stringResource(R.string.download_complete), style = MaterialTheme.typography.titleSmall)
                    }
                    s.downloadUrl?.let { url ->
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.share_save))
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.close))
                    }
                }

                s.status == "failed" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.download_failed), style = MaterialTheme.typography.titleSmall)
                    }
                    s.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.close))
                    }
                }

                else -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.downloading))
                        Text("${((s.progress ?: 0.0) * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { (s.progress ?: 0.0).toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
