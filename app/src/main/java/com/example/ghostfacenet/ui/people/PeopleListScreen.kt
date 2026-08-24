package com.example.ghostfacenet.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.db.PersonEntity
import com.example.ghostfacenet.ui.ViewModelFactory
import java.io.File

@Composable
fun PeopleListScreen(app: GhostFaceNetApp, onPersonClick: (Long) -> Unit) {
    val viewModel: PeopleViewModel = viewModel(factory = ViewModelFactory(app))
    val people by viewModel.people.collectAsState()
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    if (people.isEmpty()) {
        Text(
            "Todavía no hay personas importadas. Ve a la pestaña Importar.",
            modifier = Modifier.padding(24.dp)
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Toca a una persona para editar su nombre, agregar fotos o eliminarla.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showDeleteAllConfirm = true }) {
                Text("Eliminar todas", color = MaterialTheme.colorScheme.error)
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(people, key = { it.id }) { person ->
                PersonRow(person, onClick = { onPersonClick(person.id) })
            }
        }
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Eliminar todas las personas") },
            text = {
                Text(
                    "¿Eliminar a las ${people.size} personas cargadas y todas sus fotos? " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllConfirm = false
                    viewModel.deleteAll()
                }) { Text("Eliminar todas") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PersonRow(person: PersonEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = File(person.referenceImagePath),
            contentDescription = person.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        Text(person.name, style = MaterialTheme.typography.titleMedium)
    }
}
