package app.mediafy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import app.mediafy.data.ApiService
import app.mediafy.data.TokenStorage
import app.mediafy.ui.AuthViewModel
import app.mediafy.ui.MediafyApp
import app.mediafy.ui.theme.MediafyTheme

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStorage = TokenStorage(this)
        val apiService = ApiService(tokenStorage)
        authViewModel = ViewModelProvider(
            this,
            AuthViewModel.Factory(apiService, tokenStorage)
        )[AuthViewModel::class.java]

        setContent {
            MediafyTheme {
                MediafyApp(
                    authViewModel = authViewModel,
                    apiService = apiService,
                    intent = intent
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val data = intent.data
        if (data?.scheme == "mediafy" && data.host == "payment") {
            val success = data.pathSegments.contains("success")
            authViewModel.onPaymentResult(success)
        }
    }
}
