package com.zone.android.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zone.android.core.model.SettingsRepository
import com.zone.android.core.model.StrictnessLevel
import com.zone.android.core.model.VideoItem
import com.zone.android.core.model.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for configuring a session before calibration starts.
 */
class SessionSetupViewModel(
    private val videoId: Long,
    private val videoRepository: VideoRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionSetupUiState())
    val uiState: StateFlow<SessionSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val video = videoRepository.getVideo(videoId)
            _uiState.update { it.copy(video = video) }
        }
        viewModelScope.launch {
            settingsRepository.strictnessLevel.collect { level ->
                _uiState.update { state -> state.copy(selectedStrictness = level) }
            }
        }
        viewModelScope.launch {
            settingsRepository.cameraPermissionAsked.collect { asked ->
                _uiState.update { state -> state.copy(cameraPermissionAsked = asked) }
            }
        }
        viewModelScope.launch {
            settingsRepository.sessionIntent.collect { intent ->
                _uiState.update { state -> state.copy(sessionIntent = intent) }
            }
        }
    }

    fun selectStrictness(level: StrictnessLevel) {
        viewModelScope.launch {
            settingsRepository.setStrictnessLevel(level)
            _uiState.update { it.copy(selectedStrictness = level) }
        }
    }

    fun markCameraPermissionRequested() {
        viewModelScope.launch {
            settingsRepository.setCameraPermissionAsked(true)
        }
    }

    fun rememberVideoSelection() {
        viewModelScope.launch {
            settingsRepository.setLastVideoId(videoId)
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun updateSessionIntent(intent: String) {
        viewModelScope.launch {
            settingsRepository.setSessionIntent(intent)
            _uiState.update { it.copy(sessionIntent = intent) }
        }
    }
}

/**
 * UI state for session setup.
 */
data class SessionSetupUiState(
    val video: VideoItem? = null,
    val selectedStrictness: StrictnessLevel = StrictnessLevel.STANDARD,
    val cameraPermissionAsked: Boolean = false,
    val sessionIntent: String = "",
)
