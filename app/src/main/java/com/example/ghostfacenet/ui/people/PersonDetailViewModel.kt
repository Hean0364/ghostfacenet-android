package com.example.ghostfacenet.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.FaceRepository
import com.example.ghostfacenet.data.db.PerfilEntity
import com.example.ghostfacenet.data.db.PerfilFotoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val loading: Boolean = true,
    val person: PerfilEntity? = null,
    val photos: List<PerfilFotoEntity> = emptyList()
)

class PersonDetailViewModel(
    private val repository: FaceRepository,
    private val personId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(PersonDetailUiState())
    val state: StateFlow<PersonDetailUiState> = _state

    init {
        viewModelScope.launch {
            repository.observePerson(personId).collect { person ->
                _state.update { it.copy(loading = false, person = person) }
            }
        }
        viewModelScope.launch {
            repository.observePersonPhotos(personId).collect { photos ->
                _state.update { it.copy(photos = photos) }
            }
        }
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
