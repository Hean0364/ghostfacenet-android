package com.example.ghostfacenet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.ui.people.PeopleViewModel
import com.example.ghostfacenet.ui.recognize.RecognizeViewModel
import com.example.ghostfacenet.ui.settings.SettingsViewModel

class ViewModelFactory(private val app: GhostFaceNetApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(RecognizeViewModel::class.java) ->
            RecognizeViewModel(app.repository) as T
        modelClass.isAssignableFrom(PeopleViewModel::class.java) ->
            PeopleViewModel(app.repository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(app) as T
        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
