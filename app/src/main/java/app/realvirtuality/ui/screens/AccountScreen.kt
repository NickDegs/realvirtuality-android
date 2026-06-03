package app.realvirtuality.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.realvirtuality.R
import app.realvirtuality.data.SubscriptionTier
import app.realvirtuality.ui.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsState()
    val user = state.user ?: return
    var showSubscription by remember { mutableStateOf(false) }

    if (showSubscription) {
        SubscriptionScreen(onDismiss = { showSubscription = false }, authViewModel = authViewModel)
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.my_account)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(user.username, style = MaterialTheme.typography.titleMedium)
                            Text(user.email, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.subscription)) },
                        supportingContent = {
                            Text(when (user.subscriptionTier) {
                                SubscriptionTier.FREE -> stringResource(R.string.free_plan)
                                SubscriptionTier.AD_FREE -> stringResource(R.string.adfree_plan)
                                SubscriptionTier.FULL -> stringResource(R.string.full_plan)
                            })
                        },
                        leadingContent = {
                            Icon(
                                when (user.subscriptionTier) {
                                    SubscriptionTier.FULL -> Icons.Filled.WorkspacePremium
                                    else -> Icons.Filled.Star
                                },
                                null,
                                tint = if (user.subscriptionTier == SubscriptionTier.FULL)
                                    MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            if (user.subscriptionTier != SubscriptionTier.FULL) {
                                TextButton(onClick = { showSubscription = true }) {
                                    Text(stringResource(R.string.upgrade))
                                }
                            }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = { authViewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.logout))
                }
            }
        }
    }
}
