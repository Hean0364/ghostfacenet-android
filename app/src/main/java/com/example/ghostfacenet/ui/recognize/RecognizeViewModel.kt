package com.example.ghostfacenet.ui.recognize

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostfacenet.data.FaceRepository
import com.example.ghostfacenet.data.RecognitionOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RecognizeUiState {
    data object Idle : RecognizeUiState()
    data object Loading : RecognizeUiState()
    data class Result(val outcome: RecognitionOutcome) : RecognizeUiState()
}

class RecognizeViewModel(private val repository: FaceRepository) : ViewModel() {

    private val _state = MutableStateFlow<RecognizeUiState>(RecognizeUiState.Idle)
    val state: StateFlow<RecognizeUiState> = _state

    fun recognize(bitmap: Bitmap, threshold: Float) {
        _state.value = RecognizeUiState.Loading
        viewModelScope.launch {
            val outcome = repository.recognize(bitmap, threshold)
            _state.value = RecognizeUiState.Result(outcome)
        }
    }

    fun reset() {
        _state.value = RecognizeUiState.Idle
    }
}
