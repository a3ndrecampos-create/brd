package com.beautymanager.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.Product
import com.beautymanager.app.domain.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class ReportPeriod(val label: String, val days: Long) {
    WEEK("7 dias", 7), MONTH("30 dias", 30), YEAR("12 meses", 365)
}

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val totalSold: Double = 0.0,
    val totalProfit: Double = 0.0,
    val salesCount: Int = 0,
    val topProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Relatórios de faturamento/lucro/produtos mais vendidos. A exportação em PDF/Excel
 * fica como TODO — vale uma rodada dedicada (lib de geração de PDF no Android +
 * biblioteca de planilhas leve, já que Apache POI é pesado demais para mobile).
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init { load(ReportPeriod.MONTH) }

    fun load(period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(period = period, isLoading = true)
            val zone = ZoneId.systemDefault()
            val end = System.currentTimeMillis()
            val start = LocalDate.now().minusDays(period.days).atStartOfDay(zone).toInstant().toEpochMilli()

            val totalSold = saleRepository.getTotalSoldBetween(start, end)
            val totalProfit = saleRepository.getTotalProfitBetween(start, end)
            val salesCount = saleRepository.getSalesCountBetween(start, end)
            val topProducts = saleRepository.getTopProducts(limit = 7, sinceEpochMillis = start)

            _uiState.value = ReportsUiState(
                period = period, totalSold = totalSold, totalProfit = totalProfit,
                salesCount = salesCount, topProducts = topProducts, isLoading = false
            )
        }
    }

    fun onExportPdf() {
        // TODO: gerar PDF do relatório atual (ver skill de PDF do projeto / lib android-pdf-writer)
    }

    fun onExportExcel() {
        // TODO: gerar planilha .xlsx do relatório atual
    }
}
