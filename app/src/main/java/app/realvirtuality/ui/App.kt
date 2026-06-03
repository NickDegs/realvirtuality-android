package app.realvirtuality.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.realvirtuality.data.ApiService
import app.realvirtuality.ui.screens.AuthScreen
import app.realvirtuality.ui.screens.MainScreen

@Composable
fun RealVirtualityApp(
    authViewModel: AuthViewModel,
    apiService: ApiService,
    intent: Intent?
) {
    val state by authViewModel.state.collectAsState()

    // Handle deep link from intent
    LaunchedEffect(intent) {
        val data: Uri? = intent?.data
        if (data?.scheme == "realvirtuality" && data.host == "payment") {
            val success = data.pathSegments.contains("success")
            authViewModel.onPaymentResult(success)
        }
    }

    AnimatedContent(targetState = state.isLoading to state.user, label = "auth") { (loading, user) ->
        when {
            loading && user == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            user != null -> MainScreen(authViewModel = authViewModel, apiService = apiService)
            else -> AuthScreen(authViewModel = authViewModel)
        }
    }
}
