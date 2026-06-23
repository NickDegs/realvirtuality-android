package app.downify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.downify.R
import app.downify.data.Countries
import app.downify.data.Country
import app.downify.ui.AuthViewModel

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsState()
    var phase by remember { mutableStateOf("phone") }   // phone | code
    var country by remember { mutableStateOf(Countries.deviceDefault()) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var localNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    val fullPhone = country.dialCode + localNumber.filter { it.isDigit() }

    if (showCountryPicker) {
        CountryPickerSheet(
            selected = country,
            onSelect = { country = it; showCountryPicker = false },
            onDismiss = { showCountryPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(
            text = if (phase == "phone") stringResource(R.string.phone_login_subtitle)
                   else stringResource(R.string.code_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (phase == "phone") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showCountryPicker = true },
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("${country.flag} ${country.dialCode}", maxLines = 1)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                OutlinedTextField(
                    value = localNumber,
                    onValueChange = { localNumber = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f)
                )
            }
            state.error?.let { ErrorText(it) }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { authViewModel.sendSmsCode(fullPhone) { ok -> if (ok) phase = "code" } },
                enabled = !state.isLoading && localNumber.filter { it.isDigit() }.length >= 6,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(R.string.send_code))
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.sms_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(stringResource(R.string.verification_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let { ErrorText(it) }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { authViewModel.verifySmsCode(fullPhone, code) },
                enabled = !state.isLoading && code.filter { it.isDigit() }.length >= 4,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(R.string.verify))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = { code = ""; authViewModel.clearError(); phase = "phone" }) {
                    Text(stringResource(R.string.change_number))
                }
                TextButton(onClick = { authViewModel.sendSmsCode(fullPhone) {} }) {
                    Text(stringResource(R.string.resend_code))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.6f))
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { authViewModel.loginAsGuest() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Filled.PersonOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.continue_without_account))
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.guest_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorText(msg: String) {
    Spacer(Modifier.height(8.dp))
    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    selected: Country,
    onSelect: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) Countries.all
        else Countries.all.filter {
            it.name.lowercase().contains(q) || it.dialCode.contains(q) || it.iso.lowercase().contains(q)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxHeight(0.85f)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.country_search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filtered, key = { it.iso }) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(c) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(c.flag, style = MaterialTheme.typography.titleLarge)
                        Text(c.name, modifier = Modifier.weight(1f))
                        Text(c.dialCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (c.iso == selected.iso) {
                            Icon(Icons.Filled.Check, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
