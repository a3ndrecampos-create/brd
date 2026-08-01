package com.beautymanager.app.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.AppUser
import com.beautymanager.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Usado por telas que precisam saber "o que o usuário logado pode fazer" (ex.:
 * esconder o botão de adicionar produto para quem não tem canManageProducts).
 * Cada tela pode pedir sua própria instância via hiltViewModel() — o estado vem
 * do mesmo SessionRepository, então fica sempre consistente.
 */
@HiltViewModel
class CurrentUserViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {
    val currentUser: StateFlow<AppUser?> = sessionRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
