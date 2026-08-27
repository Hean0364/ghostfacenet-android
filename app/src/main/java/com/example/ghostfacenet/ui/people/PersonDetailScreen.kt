package com.example.ghostfacenet.ui.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostfacenet.GhostFaceNetApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PersonDetailScreen(app: GhostFaceNetApp, personId: Long, onBack: () -> Unit) {
    val viewModel: PersonDetailViewModel =
        viewModel(factory = PersonDetailViewModelFactory(app, personId))
    val state by viewModel.state.collectAsState()

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
        if (person == null) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else {
                Text("Este perfil ya no existe.", modifier = Modifier.padding(24.dp))
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileImage(
                    reference = person.fotoPerfil,
                    contentDescription = person.nombre,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text("ID: ${person.id}", style = MaterialTheme.typography.labelLarge)
                    Text("Nombre: ${person.nombre}")
                    Text("Estado: ${person.estado}")
                }
            }

            Text(
                "Creado: ${formatDate(person.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Actualizado: ${formatDate(person.updateAt)}",
                style = MaterialTheme.typography.bodySmall
            )

            val photoReferences = buildList {
                if (person.fotoPerfil.isNotBlank()) add(person.fotoPerfil)
                addAll(state.photos.map { it.fotoBase64 })
            }

            Text(
                "Fotos de reconocimiento (${photoReferences.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            if (photoReferences.isEmpty()) {
                Text(
                    "No hay fotos almacenadas para este perfil.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(photoReferences, key = { index, _ -> index }) { _, reference ->
                        ProfileImage(
                            reference = reference,
                            contentDescription = "Foto de reconocimiento",
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
