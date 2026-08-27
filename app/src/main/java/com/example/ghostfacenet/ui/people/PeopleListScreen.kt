package com.example.ghostfacenet.ui.people

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.db.PerfilEntity
import com.example.ghostfacenet.ui.ViewModelFactory

@Composable
fun PeopleListScreen(app: GhostFaceNetApp, onPersonClick: (Long) -> Unit) {
    val viewModel: PeopleViewModel = viewModel(factory = ViewModelFactory(app))
    val people by viewModel.people.collectAsState()

    if (people.isEmpty()) {
        Text(
            "No hay perfiles disponibles en la base local.",
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
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                "Perfiles cargados desde la base local. Toca una persona para consultar su detalle.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(people, key = { it.id }) { person ->
                PersonRow(person, onClick = { onPersonClick(person.id) })
            }
        }
    }
}

@Composable
private fun PersonRow(person: PerfilEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileImage(
            reference = person.fotoPerfil,
            contentDescription = person.nombre,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
        Column {
            Text("#${person.id} · ${person.nombre}", style = MaterialTheme.typography.titleMedium)
            Text(
                person.estado,
                color = if (person.estado == "Activo") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
