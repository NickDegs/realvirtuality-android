package app.downify.ui.screens

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import app.downify.R
import app.downify.data.ApiService
import app.downify.ui.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    apiService: ApiService,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rv_settings", Context.MODE_PRIVATE) }
    var quality by remember { mutableStateOf(prefs.getString("quality", "best") ?: "best") }
    var audioOnly by remember { mutableStateOf(prefs.getBoolean("audio_only", false)) }
    var showSubscription by remember { mutableStateOf(false) }

    if (showSubscription) {
        SubscriptionScreen(onDismiss = { showSubscription = false }, authViewModel = authViewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.quality)) },
                    trailingContent = {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = when (quality) {
                                    "best" -> stringResource(R.string.best)
                                    else -> "$quality p"
                                },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor().width(IntrinsicSize.Min)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("best" to stringResource(R.string.best), "1080" to "1080p",
                                    "720" to "720p", "480" to "480p").forEach { (v, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        quality = v
                                        prefs.edit().putString("quality", v).apply()
                                        expanded = false
                                    })
                                }
                            }
                        }
                    }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.audio_only)) },
                    trailingContent = {
                        Switch(checked = audioOnly, onCheckedChange = {
                            audioOnly = it
                            prefs.edit().putBoolean("audio_only", it).apply()
                        })
                    }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.manage_plan)) },
                    modifier = Modifier.clickable { showSubscription = true }
                )
                HorizontalDivider()
            }

            item {
                val versionName = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrDefault("1.0")
                ListItem(
                    headlineContent = { Text(stringResource(R.string.version)) },
                    trailingContent = { Text(versionName ?: "1.0") }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.privacy_policy)) },
                    modifier = Modifier.clickable {
                        openUrl(context, "https://downify.app/privacy")
                    }
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.support)) },
                    modifier = Modifier.clickable {
                        openUrl(context, "https://downify.app/support")
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    runCatching {
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
