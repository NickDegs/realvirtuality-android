package app.realvirtuality

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import app.realvirtuality.data.ApiService
import app.realvirtuality.data.TokenStorage
import app.realvirtuality.ui.AuthViewModel
import app.realvirtuality.ui.RealVirtualityApp
import app.realvirtuality.ui.theme.RealVirtualityTheme

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
            RealVirtualityTheme {
                RealVirtualityApp(
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
        if (data?.scheme == "realvirtuality" && data.host == "payment") {
            val success = data.pathSegments.contains("success")
            authViewModel.onPaymentResult(success)
        }
    }
}
