package app.downify.ui.screens

import android.app.Activity
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
import app.downify.R
import app.downify.billing.BillingManager
import app.downify.billing.StoreProduct
import app.downify.data.ApiService
import app.downify.data.TokenStorage
import app.downify.ui.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val billing = remember {
        BillingManager(
            context = context.applicationContext,
            api = ApiService(TokenStorage(context.applicationContext)),
            scope = scope,
            onEntitlementChanged = { authViewModel.refreshUser() }
        )
    }
    DisposableEffect(Unit) {
        billing.start()
        onDispose { billing.release() }
    }

    val products by billing.products.collectAsState()
    val purchasing by billing.purchasing.collectAsState()
    val status by billing.status.collectAsState()

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

            if (products.isEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }

            items(products) { product ->
                PlanCard(
                    product = product,
                    isBuying = purchasing == product.key
                ) {
                    activity?.let { billing.purchase(it, product) }
                }
            }

            status?.let {
                item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            item {
                TextButton(
                    onClick = { billing.restore() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.restore_purchases)) }
            }

            item {
                Text(
                    text = stringResource(R.string.subscription_terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanCard(product: StoreProduct, isBuying: Boolean, onBuy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.title, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(product.price, style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary)
                        if (product.period.isNotBlank()) {
                            Text(product.period, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Button(onClick = onBuy, enabled = !isBuying) {
                    if (isBuying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.buy))
                    }
                }
            }
        }
    }
}
