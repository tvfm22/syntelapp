package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppWorkspace
import com.example.data.local.ConnectivityObserver
import com.example.data.local.SessionStore
import com.example.data.local.SyntelDatabase
import com.example.data.local.UserSession
import com.example.data.remote.CafeOptionDto
import com.example.data.remote.NetworkModule
import com.example.data.repository.SyntelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class SyntelAppViewModel(application: Application) : AndroidViewModel(application) {

    val sessionStore = SessionStore(application)
    val database = SyntelDatabase.getInstance(application)
    val api = NetworkModule.createApiService { sessionStore.cachedToken }
    val repository = SyntelRepository(api, database.syntelDao(), sessionStore)
    val connectivityObserver = ConnectivityObserver(application)

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    val session: StateFlow<UserSession> = repository.sessionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSession(),
    )

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeMessage() {
        _snackbarMessage.value = null
    }

    fun login(username: String, password: String, cafeCode: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState(errorMessage = "نام کاربری و رمز عبور را وارد کنید.")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginUiState(isLoading = true, errorMessage = null)
            val result = repository.login(username, password, cafeCode)
            result.fold(
                onSuccess = {
                    _loginState.value = LoginUiState(isLoading = false, errorMessage = null)
                },
                onFailure = { err ->
                    _loginState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = err.message ?: "خطا در ورود به حساب کاربری.",
                    )
                },
            )
        }
    }

    fun clearLoginError() {
        _loginState.value = _loginState.value.copy(errorMessage = null)
    }

    fun refreshProfile() {
        if (!isOnline.value) return
        viewModelScope.launch {
            repository.refreshProfile()
        }
    }

    fun switchWorkspace(workspace: AppWorkspace) {
        viewModelScope.launch {
            repository.setWorkspace(workspace)
        }
    }

    fun switchCafe(cafe: CafeOptionDto) {
        viewModelScope.launch {
            repository.setCafe(cafe)
            showMessage("شعبه فعال به «${cafe.name}» تغییر یافت.")
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            showMessage("از حساب کاربری خارج شدید.")
        }
    }
}
