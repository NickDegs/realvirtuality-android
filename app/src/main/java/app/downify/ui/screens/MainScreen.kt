package app.downify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.downify.R
import app.downify.data.ApiService
import app.downify.ui.AuthViewModel

@Composable
fun MainScreen(authViewModel: AuthViewModel, apiService: ApiService) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Download, null) },
                    label = { Text(stringResource(R.string.download)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Layers, null) },
                    label = { Text("Toplu") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.PhotoLibrary, null) },
                    label = { Text("Galeri") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.MoreHoriz, null) },
                    label = { Text("Daha Fazla") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Filled.Person, null) },
                    label = { Text(stringResource(R.string.my_account)) }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.padding(paddingValues),
                authViewModel = authViewModel,
                apiService = apiService
            )
            1 -> BulkDownloadScreen(
                modifier = Modifier.padding(paddingValues),
                authViewModel = authViewModel,
                apiService = apiService
            )
            2 -> GalleryScreen(
                modifier = Modifier.padding(paddingValues),
                authViewModel = authViewModel,
                apiService = apiService
            )
            3 -> MoreScreen(
                modifier = Modifier.padding(paddingValues),
                authViewModel = authViewModel,
                apiService = apiService
            )
            4 -> AccountScreen(
                modifier = Modifier.padding(paddingValues),
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
private fun MoreScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    apiService: ApiService
) {
    var currentScreen by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        "auto" -> AutoDownloadScreen(
            modifier = modifier,
            authViewModel = authViewModel,
            apiService = apiService
        )
        "scheduled" -> ScheduledDownloadScreen(
            apiService = apiService,
            authViewModel = authViewModel
        )
        else -> MoreMenu(modifier = modifier) { currentScreen = it }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenu(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Daha Fazla") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoreMenuItem(
                icon = { Icon(Icons.Default.Schedule, null) },
                title = "Otomatik İndirme",
                subtitle = "Kanal/profil yeni video paylaşınca otomatik indir",
                onClick = { onNavigate("auto") }
            )
            MoreMenuItem(
                icon = { Icon(Icons.Default.CalendarMonth, null) },
                title = "Planlı İndirmeler",
                subtitle = "Belirli bir saatte indirilecek videoları yönet",
                onClick = { onNavigate("scheduled") }
            )
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.size(40.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                icon()
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
