package com.example.ghostfacenet.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostfacenet.data.FaceRepository
import com.example.ghostfacenet.data.db.PerfilEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PeopleViewModel(private val repository: FaceRepository) : ViewModel() {

    val people: StateFlow<List<PerfilEntity>> = repository.observePeople()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
