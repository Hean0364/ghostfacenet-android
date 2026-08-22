package com.example.ghostfacenet.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.db.PersonEntity
import com.example.ghostfacenet.ui.ViewModelFactory
import java.io.File

@Composable
fun PeopleListScreen(app: GhostFaceNetApp) {
    val viewModel: PeopleViewModel = viewModel(factory = ViewModelFactory(app))
    val people by viewModel.people.collectAsState()

    if (people.isEmpty()) {
        Text(
            "Todavía no hay personas importadas. Ve a la pestaña Importar.",
            modifier = Modifier.padding(24.dp)
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(people, key = { it.id }) { person ->
            PersonRow(person)
        }
    }
}

@Composable
private fun PersonRow(person: PersonEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
