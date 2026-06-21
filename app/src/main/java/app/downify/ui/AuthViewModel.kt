package app.downify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.downify.data.ApiException
import app.downify.data.ApiService
import app.downify.data.TokenStorage
import app.downify.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val api: ApiService,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val isAuthenticated: Boolean get() = _state.value.user != null

    init {
        tryAutoLogin()
    }

    fun tryAutoLogin() {
        if (tokenStorage.loadToken() == null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { api.getMe() }
                .onSuccess { _state.value = AuthUiState(user = it) }
                .onFailure {
                    tokenStorage.deleteToken()
                    _state.value = AuthUiState()
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { api.login(email, password) }
                .onSuccess {
                    tokenStorage.saveToken(it.accessToken)
                    _state.value = AuthUiState(user = it.user)
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoading = false, error = it.localizedMessage)
                }
        }
    }

    fun register(email: String, username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { api.register(email, username, password) }
                .onSuccess {
                    tokenStorage.saveToken(it.accessToken)
                    _state.value = AuthUiState(user = it.user)
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoading = false, error = it.localizedMessage)
                }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { api.guestLogin() }
                .onSuccess {
                    tokenStorage.saveToken(it.accessToken)
                    _state.value = AuthUiState(user = it.user)
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoading = false, error = it.localizedMessage)
                }
        }
    }

    fun logout() {
        tokenStorage.deleteToken()
        _state.value = AuthUiState()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { api.deleteAccount() }
                .onSuccess {
                    tokenStorage.deleteToken()
                    _state.value = AuthUiState()
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoading = false, error = it.localizedMessage)
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refreshUser() {
        viewModelScope.launch {
            runCatching { api.getMe() }
                .onSuccess { _state.value = _state.value.copy(user = it) }
                .onFailure { if (it is ApiException.Unauthorized) logout() }
        }
    }

    fun onPaymentResult(success: Boolean) {
        if (success) refreshUser()
    }

    class Factory(private val api: ApiService, private val tokenStorage: TokenStorage) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(api, tokenStorage) as T
        }
    }
}
