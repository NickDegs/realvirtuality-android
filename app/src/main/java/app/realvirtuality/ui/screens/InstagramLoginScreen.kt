package app.realvirtuality.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import app.realvirtuality.R
import app.realvirtuality.data.ApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramLoginScreen(apiService: ApiService, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (savedSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Başarılı!") },
            text = { Text(stringResource(R.string.instagram_saved)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.instagram_login)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                if (url.contains("instagram.com") &&
                                    !url.contains("login") &&
                                    !url.contains("accounts")) {
                                    val cookies = CookieManager.getInstance()
                                        .getCookie("https://www.instagram.com") ?: return
                                    scope.launch {
                                        isSaving = true
                                        runCatching { apiService.saveInstagramSession(cookies) }
                                            .onSuccess { savedSuccess = true }
                                            .onFailure { errorMessage = it.localizedMessage }
                                        isSaving = false
                                    }
                                }
                            }
                        }
                        loadUrl("https://www.instagram.com/accounts/login/")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isSaving) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card { Box(Modifier.padding(24.dp)) { CircularProgressIndicator() } }
                }
            }

            errorMessage?.let {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = { TextButton(onClick = { errorMessage = null }) { Text(stringResource(R.string.ok)) } }
                ) { Text(it) }
            }
        }
    }
}
