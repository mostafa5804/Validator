package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BankInfo
import com.example.data.BankRepository
import com.example.data.CityRepository
import com.example.data.Validator
import com.example.data.history.HistoryDao
import com.example.data.history.HistoryItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ValidationType {
    CARD, SHABA, NATIONAL_ID
}

data class ValidationState(
    val input: String = "",
    val type: ValidationType = ValidationType.CARD,
    val isValid: Boolean? = null,
    val bankInfo: BankInfo? = null,
    val city: String? = null,
    val message: String = "",
    val isLoading: Boolean = false
)

class MainViewModel(
    private val cityRepository: CityRepository,
    private val historyDao: HistoryDao
) : ViewModel() {
    private val _state = MutableStateFlow(ValidationState())
    val state: StateFlow<ValidationState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryItem>> = historyDao.getRecentHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var validationJob: Job? = null

    val validCount = MutableStateFlow(0)
    val invalidCount = MutableStateFlow(0)

    fun onTypeChanged(type: ValidationType) {
        validationJob?.cancel()
        _state.update { it.copy(type = type, input = "", isValid = null, bankInfo = null, city = null, message = "", isLoading = false) }
    }

    fun forceValidate() {
        val sanitized = _state.value.input
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(100)
            validate(sanitized, _state.value.type)
        }
    }

    fun onInputChanged(input: String) {
        val sanitized = input.uppercase().replace("IR", "").replace(Regex("[^0-9]"), "")
        _state.update { it.copy(input = sanitized, isValid = null, bankInfo = null, city = null, message = "", isLoading = false) }
        
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            val len = sanitized.length
            val targetLen = when (_state.value.type) {
                ValidationType.CARD -> 16
                ValidationType.SHABA -> 24
                ValidationType.NATIONAL_ID -> 10
            }

            if (len >= targetLen) {
                _state.update { it.copy(isLoading = true) }
                delay(600) // Simulated loading animation delay
                validate(sanitized, _state.value.type)
            }
        }
    }

    private suspend fun validate(input: String, type: ValidationType) {
        val isValid: Boolean
        var bank: BankInfo? = null
        var city: String? = null
        var msg = ""

        when (type) {
            ValidationType.CARD -> {
                isValid = Validator.validateCardNumber(input)
                bank = if (isValid) BankRepository.findBankByCard(input) else null
                msg = if (isValid) "شماره کارت معتبر است" else "شماره کارت نامعتبر است"
            }
            ValidationType.SHABA -> {
                isValid = Validator.validateShaba(input)
                bank = if (isValid) BankRepository.findBankByShaba(input) else null
                msg = if (isValid) "شماره شبا معتبر است" else "شماره شبا نامعتبر است"
            }
            ValidationType.NATIONAL_ID -> {
                isValid = Validator.validateNationalId(input)
                city = if (isValid) cityRepository.getCityByNationalId(input) else null
                msg = if (isValid) "کد ملی معتبر - صادره از: $city" else "کد ملی نامعتبر است"
            }
        }

        if (isValid) {
            historyDao.insertItem(HistoryItem(input = input, type = type))
            historyDao.cleanupOldEntries()
            validCount.value++
        } else {
            invalidCount.value++
        }

        _state.update { 
            it.copy(
                isValid = isValid,
                bankInfo = bank,
                city = city,
                message = msg,
                isLoading = false
            ) 
        }
    }
}
