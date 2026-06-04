package app.downify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import app.downify.data.ApiService
import app.downify.data.TokenStorage
import app.downify.ui.AuthViewModel
import app.downify.ui.DownifyApp
import app.downify.ui.theme.DownifyTheme

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
            DownifyTheme {
                DownifyApp(
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
        if (data?.scheme == "downify" && data.host == "payment") {
            val success = data.pathSegments.contains("success")
            authViewModel.onPaymentResult(success)
        }
    }
}
