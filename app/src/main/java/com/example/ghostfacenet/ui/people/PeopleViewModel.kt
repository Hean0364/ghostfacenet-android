package com.example.ghostfacenet.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ghostfacenet.data.FaceRepository
import com.example.ghostfacenet.data.db.PersonEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PeopleViewModel(private val repository: FaceRepository) : ViewModel() {

    private val _people = MutableStateFlow<List<PersonEntity>>(emptyList())
    val people: StateFlow<List<PersonEntity>> = _people

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _people.value = repository.getAllPeople()
        }
    }
}
