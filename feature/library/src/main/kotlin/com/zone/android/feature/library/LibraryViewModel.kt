package com.zone.android.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zone.android.core.model.VideoItem
import com.zone.android.core.model.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the library screen.
 */
class LibraryViewModel(
    private val videoRepository: VideoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            videoRepository.observeVideos().collect { videos ->
                _uiState.update { state -> state.copy(videos = videos) }
            }
        }
    }

    fun importVideo(contentUri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }
            val result = videoRepository.importVideo(contentUri)
            _uiState.update { state ->
                state.copy(
                    isImporting = false,
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}

/**
 * UI state for the library screen.
 */
data class LibraryUiState(
    val videos: List<VideoItem> = emptyList(),
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
)
