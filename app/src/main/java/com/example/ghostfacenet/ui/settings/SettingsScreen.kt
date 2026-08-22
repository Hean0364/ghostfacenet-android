package com.example.ghostfacenet.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.ui.ViewModelFactory

@Composable
fun SettingsScreen(app: GhostFaceNetApp, onThresholdChange: (Float) -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(app))
    val threshold by viewModel.threshold.collectAsState()

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Umbral de reconocimiento: ${"%.2f".format(threshold)}",
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        Slider(
            value = threshold,
            onValueChange = {
                viewModel.setThreshold(it)
                onThresholdChange(it)
            },
            valueRange = 0f..1f
        )
        Text(
            "Un umbral más alto reduce falsos positivos, pero puede rechazar coincidencias válidas. " +
                "Valor por defecto calibrado contra el benchmark LFW: 0.30."
        )
    }
}
