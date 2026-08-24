package com.example.ghostfacenet.ui.people

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.FaceRepository
import com.example.ghostfacenet.data.db.FaceEmbeddingEntity
import com.example.ghostfacenet.data.db.PersonEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val loading: Boolean = true,
    val person: PersonEntity? = null,
    val photos: List<FaceEmbeddingEntity> = emptyList(),
    val addPhotoFailed: Boolean = false,
    val personDeleted: Boolean = false
)

class PersonDetailViewModel(
    private val repository: FaceRepository,
    private val personId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(PersonDetailUiState())
    val state: StateFlow<PersonDetailUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val person = repository.getPerson(personId)
            val photos = repository.getPersonPhotos(personId)
            _state.value = _state.value.copy(loading = false, person = person, photos = photos)
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            repository.renamePerson(personId, newName)
            refresh()
        }
    }

    fun deletePhoto(embeddingId: Long) {
        viewModelScope.launch {
            val personDeleted = repository.deletePhoto(personId, embeddingId)
            if (personDeleted) {
                _state.value = _state.value.copy(personDeleted = true)
            } else {
                refresh()
            }
        }
    }

    fun deletePerson() {
        viewModelScope.launch {
            repository.deletePerson(personId)
            _state.value = _state.value.copy(personDeleted = true)
        }
    }

    fun addPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val ok = repository.addPhotoToPerson(personId, bitmap)
            _state.value = _state.value.copy(addPhotoFailed = !ok)
            refresh()
        }
    }

    fun consumeAddPhotoFailed() {
        _state.value = _state.value.copy(addPhotoFailed = false)
    }
}

class PersonDetailViewModelFactory(
    private val app: GhostFaceNetApp,
    private val personId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonDetailViewModel::class.java)) {
            return PersonDetailViewModel(app.repository, personId) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
