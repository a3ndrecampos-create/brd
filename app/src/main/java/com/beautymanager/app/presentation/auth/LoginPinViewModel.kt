package com.beautymanager.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.AppUser
import com.beautymanager.app.domain.model.UserRole
import com.beautymanager.app.domain.repository.SessionRepository
import com.beautymanager.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val pin: String = "",
    val needsFirstTimeSetup: Boolean = false,
    val errorMessage: String? = null,
    val biometricAvailableAndEnabled: Boolean = false,
    val isChecking: Boolean = false
)

@HiltViewModel
class LoginPinViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val alreadyConfigured = sessionRepository.isAnyUserConfigured()
            val biometric = sessionRepository.isBiometricEnabled()
            _uiState.value = _uiState.value.copy(
                needsFirstTimeSetup = !alreadyConfigured,
                biometricAvailableAndEnabled = biometric
            )
        }
    }

    fun onDigitPressed(digit: String) {
        val current = _uiState.value
        if (current.pin.length >= 6) return
        _uiState.value = current.copy(pin = current.pin + digit, errorMessage = null)
    }

    fun onBackspace() {
        _uiState.value = _uiState.value.copy(pin = _uiState.value.pin.dropLast(1), errorMessage = null)
    }

    /**
     * Primeiro acesso: não existe nenhum usuário ainda, então o PIN digitado vira o
     * PIN do administrador (dono da loja), que depois pode cadastrar funcionários em
     * Configurações > Usuários.
     */
    fun onConfirmFirstTimeSetup(onDone: () -> Unit) {
        val pin = _uiState.value.pin
        if (pin.length < 4) {
            _uiState.value = _uiState.value.copy(errorMessage = "O PIN precisa ter pelo menos 4 dígitos")
            return
        }
        viewModelScope.launch {
            userRepository.upsert(
                AppUser(
                    name = "Administrador",
                    role = UserRole.ADMIN,
                    canManageProducts = true,
                    canManageSales = true,
                    canViewReports = true,
                    canManageUsers = true
                ),
                pin = pin
            )
            sessionRepository.loginWithPin(pin)
            onDone()
        }
    }

    fun onSubmitLogin(onSuccess: () -> Unit) {
        val pin = _uiState.value.pin
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true)
            val user = sessionRepository.loginWithPin(pin)
            if (user != null) {
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(pin = "", errorMessage = "PIN incorreto", isChecking = false)
            }
        }
    }
}
