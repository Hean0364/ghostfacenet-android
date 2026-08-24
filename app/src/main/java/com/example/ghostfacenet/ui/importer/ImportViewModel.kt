package com.example.ghostfacenet.ui.importer

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostfacenet.data.FaceRepository
import com.example.ghostfacenet.data.ImportSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ImportUiState {
    data object Idle : ImportUiState()
    data object Loading : ImportUiState()
    data class Done(val summary: ImportSummary) : ImportUiState()
}

class ImportViewModel(private val repository: FaceRepository) : ViewModel() {

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state

    fun importFromFolder(treeUri: Uri) {
        _state.value = ImportUiState.Loading
        viewModelScope.launch {
            val summary = repository.importPhotosFromTree(treeUri)
            _state.value = ImportUiState.Done(summary)
        }
    }

    fun importForPerson(personName: String, bitmaps: List<Bitmap>) {
        _state.value = ImportUiState.Loading
        viewModelScope.launch {
            val summary = repository.importPhotosForPerson(personName, bitmaps)
            _state.value = ImportUiState.Done(summary)
        }
    }
}
