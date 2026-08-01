package com.tapago.feature.photoshare.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapago.core.common.Outcome
import com.tapago.feature.photoshare.data.InstagramStoriesSharer
import com.tapago.feature.photoshare.data.StatsOverlayComposer
import com.tapago.feature.photoshare.domain.BuildRunPhotoStatsUseCase
import com.tapago.feature.photoshare.domain.RunPhotoStats
import com.tapago.feature.tracking.domain.RunSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoShareUiState(
    val stats: RunPhotoStats? = null,
    val composedImageUri: Uri? = null,
    val isSharing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PhotoShareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: RunSessionRepository,
    private val buildRunPhotoStats: BuildRunPhotoStatsUseCase,
    private val overlayComposer: StatsOverlayComposer,
    private val instagramSharer: InstagramStoriesSharer,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(PhotoShareUiState())
    val uiState: StateFlow<PhotoShareUiState> = _uiState.asStateFlow()

    init {
        val session = sessionRepository.getById(sessionId)
        if (session != null) {
            _uiState.update { it.copy(stats = buildRunPhotoStats(session)) }
        } else {
            _uiState.update { it.copy(errorMessage = "Sessão não encontrada") }
        }
    }

    /** Chamado quando a foto é capturada pela câmera; aplica o overlay de estatísticas. */
    fun onPhotoCaptured(rawPhoto: Bitmap) {
        val stats = _uiState.value.stats ?: return
        val composed = overlayComposer.compose(rawPhoto, stats)
        when (val result = instagramSharer.saveToCache(composed)) {
            is Outcome.Success -> _uiState.update { it.copy(composedImageUri = result.data) }
            is Outcome.Error -> _uiState.update { it.copy(errorMessage = result.message) }
            Outcome.Loading -> Unit
        }
    }

    fun onShareToInstagramClicked() {
        val uri = _uiState.value.composedImageUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            when (val result = instagramSharer.shareToStories(uri)) {
                is Outcome.Success -> _uiState.update { it.copy(isSharing = false) }
                is Outcome.Error -> _uiState.update {
                    it.copy(isSharing = false, errorMessage = result.message)
                }
                Outcome.Loading -> Unit
            }
        }
    }
}
