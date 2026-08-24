package com.example.ghostfacenet.ui.people

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.db.FaceEmbeddingEntity
import com.example.ghostfacenet.ui.loadBitmapFromUri
import java.io.File

@Composable
fun PersonDetailScreen(app: GhostFaceNetApp, personId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: PersonDetailViewModel =
        viewModel(factory = PersonDetailViewModelFactory(app, personId))
    val state by viewModel.state.collectAsState()

    var showDeletePersonConfirm by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<FaceEmbeddingEntity?>(null) }
    var showAddPhotoError by remember { mutableStateOf(false) }

    LaunchedEffect(state.personDeleted) {
        if (state.personDeleted) onBack()
    }

    LaunchedEffect(state.addPhotoFailed) {
        if (state.addPhotoFailed) {
            showAddPhotoError = true
            viewModel.consumeAddPhotoFailed()
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            loadBitmapFromUri(context, uri)?.let { viewModel.addPhoto(it) }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            loadBitmapFromUri(context, uri)?.let { viewModel.addPhoto(it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Volver") }
        }

        val person = state.person
        if (person == null && !state.loading) {
            Text("Esta persona ya no existe.", modifier = Modifier.padding(24.dp))
            return@Column
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            person?.let {
                AsyncImage(
                    model = File(it.referenceImagePath),
                    contentDescription = it.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )

                var name by remember(it.id, it.name) { mutableStateOf(it.name) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = name.isNotBlank() && name != it.name,
                        onClick = { viewModel.rename(name) }
                    ) {
                        Text("Guardar")
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Button(onClick = {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Text("Agregar foto")
                }
                Button(onClick = { pickFileLauncher.launch(arrayOf("image/*")) }) {
                    Text("Elegir archivo")
                }
            }

            if (showAddPhotoError) {
                Text(
                    "No se detectó ningún rostro en esa foto; no se agregó.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                "Fotos (${state.photos.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.photos, key = { it.id }) { photo ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { photoToDelete = photo }
                ) {
                    AsyncImage(
                        model = File(photo.sourceImagePath),
                        contentDescription = "Foto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .background(
                                MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                            .size(22.dp)
                            .clickable { photoToDelete = photo },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showDeletePersonConfirm = true }) {
                Text("Eliminar persona", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Eliminar foto") },
            text = { Text("¿Eliminar esta foto? Si es la última que le queda a la persona, también se eliminará a la persona.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePhoto(photo.id)
                    photoToDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showDeletePersonConfirm) {
        AlertDialog(
            onDismissRequest = { showDeletePersonConfirm = false },
            title = { Text("Eliminar persona") },
            text = { Text("¿Eliminar a ${state.person?.name ?: "esta persona"} y todas sus fotos? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeletePersonConfirm = false
                    viewModel.deletePerson()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePersonConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}
