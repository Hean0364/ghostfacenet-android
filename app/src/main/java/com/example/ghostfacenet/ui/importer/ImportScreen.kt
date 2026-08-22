package com.example.ghostfacenet.ui.importer

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.ui.ViewModelFactory

@Composable
fun ImportScreen(app: GhostFaceNetApp) {
    val context = LocalContext.current
    val viewModel: ImportViewModel = viewModel(factory = ViewModelFactory(app))
    val state by viewModel.state.collectAsState()

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.importFromFolder(uri)
        }
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)) {
        Text("Importar fotos", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Organiza tus fotos en una subcarpeta por persona (ej. Fotos/Juan/1.jpg). " +
                "Si eliges una carpeta sin subcarpetas, cada archivo se importa como una persona distinta.",
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Button(onClick = { folderLauncher.launch(null) }) {
            Text("Elegir carpeta")
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            when (val s = state) {
                is ImportUiState.Idle -> {}
                is ImportUiState.Loading -> CircularProgressIndicator()
                is ImportUiState.Done -> {
                    Text("Importación terminada.")
                    Text("Fotos encontradas: ${s.summary.totalImages}")
                    Text("Rostros importados: ${s.summary.imported}")
                    Text("Sin rostro detectado: ${s.summary.skippedNoFace}")
                }
            }
        }
    }
}
