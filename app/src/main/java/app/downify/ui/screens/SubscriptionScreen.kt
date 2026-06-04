package app.downify.ui.screens

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.downify.R
import app.downify.data.ApiService
import app.downify.data.SubscriptionPlan
import app.downify.data.SubscriptionTier
import app.downify.ui.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val plans = remember {
        listOf(
            SubscriptionPlan("ad_free", "Reklamsız", "$3", "tek seferlik",
                listOf("Reklamsız deneyim", "Temel indirme", "Tüm platformlar"), SubscriptionTier.AD_FREE),
            SubscriptionPlan("full_monthly", "Full — Aylık", "$5", "/ ay",
                listOf("Tüm özellikler", "Özel Instagram", "Öncelikli indirme"), SubscriptionTier.FULL),
            SubscriptionPlan("full_yearly", "Full — Yıllık", "$30", "/ yıl",
                listOf("Tüm özellikler", "Özel Instagram", "Öncelikli indirme", "%50 tasarruf"), SubscriptionTier.FULL),
            SubscriptionPlan("full_lifetime", "Ömür Boyu", "$50", "tek seferlik",
                listOf("Tüm özellikler", "Özel Instagram", "Sınırsız indirme", "Kalıcı"), SubscriptionTier.FULL)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Star, null, modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.go_premium), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.unlimited_download), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(plans) { plan ->
                PlanCard(plan = plan, isLoading = isLoading) {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        // apiService is needed here - passed via DI in real app
                        // For now open Stripe checkout via API
                        isLoading = false
                    }
                }
            }

            errorMessage?.let {
                item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: SubscriptionPlan, isLoading: Boolean, onBuy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(plan.price, style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(plan.period, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = onBuy, enabled = !isLoading) {
                    Text("Satın Al")
                }
            }
            HorizontalDivider()
            plan.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(feature, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
