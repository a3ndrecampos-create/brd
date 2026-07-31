package com.beautymanager.app.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.Customer
import com.beautymanager.app.domain.model.Product
import com.beautymanager.app.domain.model.Reminder
import com.beautymanager.app.domain.model.ReminderRule
import com.beautymanager.app.domain.model.ReminderStatus
import com.beautymanager.app.domain.repository.CustomerRepository
import com.beautymanager.app.domain.repository.ProductRepository
import com.beautymanager.app.domain.repository.ReminderRepository
import com.beautymanager.app.domain.repository.ReminderRuleRepository
import com.beautymanager.app.domain.usecase.BuildWhatsAppMessageUseCase
import com.beautymanager.app.domain.usecase.GenerateRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderRow(
    val reminder: Reminder,
    val customer: Customer?,
    val product: Product?,
    val defaultMessage: String
)

data class RemindersUiState(
    val rows: List<ReminderRow> = emptyList(),
    val isGenerating: Boolean = false
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderRuleRepository: ReminderRuleRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val generateRemindersUseCase: GenerateRemindersUseCase,
    private val buildWhatsAppMessageUseCase: BuildWhatsAppMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reminderRepository.observePending().collect { reminders ->
                val rules = reminderRuleRepository.observeAll().first().associateBy { it.id }
                val rows = reminders.map { reminder ->
                    val customer = customerRepository.getById(reminder.customerId)
                    val product = productRepository.getById(reminder.productId)
                    val rule = rules[reminder.ruleId]
                    val message = if (customer != null && product != null && rule != null) {
                        buildWhatsAppMessageUseCase(customer.name, product.name, rule.messageTemplate)
                    } else ""
                    ReminderRow(reminder, customer, product, message)
                }
                _uiState.value = _uiState.value.copy(rows = rows)
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            generateRemindersUseCase()
            _uiState.value = _uiState.value.copy(isGenerating = false)
        }
    }

    fun onDismiss(reminderId: Long) {
        viewModelScope.launch { reminderRepository.markStatus(reminderId, ReminderStatus.IGNORADO) }
    }

    fun onMarkSent(reminderId: Long) {
        viewModelScope.launch { reminderRepository.markStatus(reminderId, ReminderStatus.ENVIADO) }
    }
}
