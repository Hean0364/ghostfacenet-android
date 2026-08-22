package com.example.ghostfacenet.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.ghostfacenet.data.ThresholdPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val _threshold = MutableStateFlow(ThresholdPreferences.get(context))
    val threshold: StateFlow<Float> = _threshold

    fun setThreshold(value: Float) {
        _threshold.value = value
        ThresholdPreferences.set(context, value)
    }
}
