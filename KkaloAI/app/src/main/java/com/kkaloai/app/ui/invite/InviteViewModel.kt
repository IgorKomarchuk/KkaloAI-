package com.kkaloai.app.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.data.remote.ReferralRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InviteUiState(
    val code: String = "",
    val count: Int = 0
)

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val referralRepository: ReferralRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteUiState())
    val uiState: StateFlow<InviteUiState> = _uiState

    init {
        viewModelScope.launch {
            val code = referralRepository.ensureCode()
            val count = userPreferences.referralCount.first()
            _uiState.value = InviteUiState(code = code, count = count)
        }

        viewModelScope.launch {
            combine(userPreferences.myReferralCode, userPreferences.referralCount) { c, n ->
                InviteUiState(code = c ?: "", count = n)
            }.collect { _uiState.value = it }
        }
    }
}
