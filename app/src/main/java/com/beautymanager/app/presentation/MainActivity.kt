package com.beautymanager.app.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.AppUser
import com.beautymanager.app.domain.repository.SessionRepository
import com.beautymanager.app.presentation.navigation.BeautyManagerNavHost
import com.beautymanager.app.presentation.theme.BeautyManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: AppUser) : SessionState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionRepository.observeCurrentUser()
        .map { user -> if (user != null) SessionState.LoggedIn(user) else SessionState.LoggedOut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeautyManagerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val sessionState by viewModel.sessionState.collectAsState()
                    when (val state = sessionState) {
                        SessionState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is SessionState.LoggedIn -> BeautyManagerNavHost(isLoggedIn = true)
                        SessionState.LoggedOut -> BeautyManagerNavHost(isLoggedIn = false)
                    }
                }
            }
        }
    }
}
