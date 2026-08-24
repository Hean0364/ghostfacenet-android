package com.example.ghostfacenet.ui.importer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.ui.loadBitmapFromUri
import com.example.ghostfacenet.ui.ViewModelFactory

@Composable
fun ImportScreen(app: GhostFaceNetApp) {
    val context = LocalContext.current
    val viewModel: ImportViewModel = viewModel(factory = ViewModelFactory(app))
    val state by viewModel.state.collectAsState()
    var personName by rememberSaveable { mutableStateOf("") }
    var selectedBitmaps by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }

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

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null && personName.isNotBlank()) {
            selectedBitmaps = (selectedBitmaps + bitmap).take(MAX_PERSON_PHOTOS)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePictureLauncher.launch(null)
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PERSON_PHOTOS)
    ) { uris ->
        if (personName.isNotBlank()) {
            val remainingSlots = MAX_PERSON_PHOTOS - selectedBitmaps.size
            val bitmaps = uris.mapNotNull { loadBitmapFromUri(context, it) }
                .take(remainingSlots.coerceAtLeast(0))
            selectedBitmaps = (selectedBitmaps + bitmaps).take(MAX_PERSON_PHOTOS)
        }
    }

    val isLoading = state is ImportUiState.Loading

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)) {
        Text("Importar fotos", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Organiza tus fotos en una subcarpeta por persona (ej. Fotos/Juan/1.jpg). " +
                "Si eliges una carpeta sin subcarpetas, cada archivo se importa como una persona distinta.",
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Button(
            enabled = !isLoading,
            onClick = { folderLauncher.launch(null) }
        ) {
            Text("Elegir carpeta")
        }

        Text(
            "Registrar una persona con fotos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = personName,
            onValueChange = { personName = it },
            label = { Text("Nombre de la persona") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Puedes elegir hasta 20 fotos de la galería o tomar varias fotos nuevas.",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            enabled = personName.isNotBlank() && !isLoading,
            onClick = {
                pickImagesLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        ) {
            Text("Elegir fotos de galería")
        }
        Button(
            enabled = personName.isNotBlank() && !isLoading &&
                selectedBitmaps.size < MAX_PERSON_PHOTOS,
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    takePictureLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Tomar foto (${selectedBitmaps.size}/$MAX_PERSON_PHOTOS)")
        }
        if (selectedBitmaps.isNotEmpty()) {
            Text(
                "${selectedBitmaps.size} foto(s) listas para registrar.",
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                enabled = !isLoading,
                onClick = {
                    val photos = selectedBitmaps
                    selectedBitmaps = emptyList()
                    viewModel.importForPerson(personName, photos)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Registrar fotos seleccionadas")
            }
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

private const val MAX_PERSON_PHOTOS = 20
