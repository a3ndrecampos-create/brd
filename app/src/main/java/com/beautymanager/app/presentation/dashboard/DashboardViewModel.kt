package com.beautymanager.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.DashboardMetrics
import com.beautymanager.app.domain.usecase.DashboardMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(val metrics: DashboardMetrics) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardMetricsUseCase: DashboardMetricsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Ready(dashboardMetricsUseCase())
        }
    }
}
