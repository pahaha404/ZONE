package com.zone.android.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zone.android.core.model.SessionReport
import com.zone.android.core.model.SessionRepository
import com.zone.android.core.model.SessionStanding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the session report screen.
 */
class ReportViewModel(
    private val sessionId: Long,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState(isLoading = true))
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val report = sessionRepository.getSessionReport(sessionId)
            val standing = sessionRepository.getSessionStanding(sessionId)
            _uiState.value = ReportUiState(
                isLoading = false,
                report = report,
                standing = standing,
                errorMessage = if (report == null) "Report not found" else null,
            )
        }
    }
}

/**
 * UI state for the report screen.
 */
data class ReportUiState(
    val isLoading: Boolean = false,
    val report: SessionReport? = null,
    val standing: SessionStanding? = null,
    val errorMessage: String? = null,
)
