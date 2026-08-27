package com.example.ghostfacenet.ui.recognize

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.RecognitionOutcome
import com.example.ghostfacenet.ml.MatchResult
import com.example.ghostfacenet.ui.ViewModelFactory
import com.example.ghostfacenet.ui.loadBitmapFromUri
import com.example.ghostfacenet.ui.people.ProfileImage
import java.io.File

@Composable
fun RecognizeScreen(
    app: GhostFaceNetApp,
    threshold: Float,
    onPersonClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val viewModel: RecognizeViewModel = viewModel(factory = ViewModelFactory(app))
    val state by viewModel.state.collectAsState()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var expandedCandidate by remember { mutableStateOf<MatchResult?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraPhotoUri
        val file = cameraPhotoFile
        cameraPhotoUri = null
        cameraPhotoFile = null

        try {
            if (success && uri != null) {
                val bitmap = loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    previewBitmap = bitmap
                    viewModel.recognize(bitmap, threshold)
                }
            }
        } finally {
            file?.delete()
        }
    }

    fun launchCamera() {
        val file = File.createTempFile("recognition_", ".jpg", context.cacheDir)
        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrNull()

        if (uri == null) {
            file.delete()
            return
        }

        cameraPhotoFile = file
        cameraPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                previewBitmap = bitmap
                viewModel.recognize(bitmap, threshold)
            }
        }
    }

    // Selector de archivos (SAF): a diferencia del Photo Picker, lee directo del
    // sistema de archivos sin depender del indexado de MediaStore. Util para
    // pruebas con fotos copiadas manualmente (ej. via adb push) y como alternativa
    // si el Photo Picker no esta disponible.
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                previewBitmap = bitmap
                viewModel.recognize(bitmap, threshold)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reconocimiento Facial", style = MaterialTheme.typography.headlineSmall)

        previewBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto capturada",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            Button(onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) launchCamera()
                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }) {
                Text("Tomar foto")
            }
            Button(onClick = {
                pickImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text("Elegir de galería")
            }
            Button(onClick = { pickFileLauncher.launch(arrayOf("image/*")) }) {
                Text("Elegir archivo")
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            when (val s = state) {
                is RecognizeUiState.Idle -> Text("Toma una foto o elige una de tu galería para reconocer a alguien.")
                is RecognizeUiState.Loading -> CircularProgressIndicator()
                is RecognizeUiState.Result -> ResultCard(
                    outcome = s.outcome,
                    onPersonClick = onPersonClick,
                    onImageClick = { expandedCandidate = it }
                )
            }
        }
    }

    expandedCandidate?.let { candidate ->
        ExpandedCandidateImage(
            candidate = candidate,
            onDismiss = { expandedCandidate = null }
        )
    }
}

@Composable
private fun ResultCard(
    outcome: RecognitionOutcome,
    onPersonClick: (Long) -> Unit,
    onImageClick: (MatchResult) -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (outcome) {
                is RecognitionOutcome.Match -> {
                    PossibleMatchesList(
                        candidates = listOf(outcome.result) + outcome.alternatives,
                        onPersonClick = onPersonClick,
                        onImageClick = onImageClick
                    )
                }
                is RecognitionOutcome.NoMatch -> {
                    PossibleMatchesList(
                        candidates = outcome.closest,
                        onPersonClick = onPersonClick,
                        onImageClick = onImageClick
                    )
                }
                is RecognitionOutcome.NoFaceDetected ->
                    Text("No se detectó ningún rostro en la imagen.")
                is RecognitionOutcome.EmptyDatabase ->
                    Text("No hay embeddings disponibles en la base local.")
            }
        }
    }
}

@Composable
private fun PossibleMatchesList(
    candidates: List<MatchResult>,
    onPersonClick: (Long) -> Unit,
    onImageClick: (MatchResult) -> Unit
) {
    Text("Posibles coincidencias", style = MaterialTheme.typography.titleMedium)
    if (candidates.isEmpty()) {
        Text(
            "No se encontraron posibles coincidencias.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        return
    }

    candidates.forEach { candidate ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileImage(
                reference = candidate.referenceImageBase64,
                contentDescription = candidate.personName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onImageClick(candidate) }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPersonClick(candidate.personId) }
            ) {
                Text(candidate.personName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Similitud: ${"%.3f".format(candidate.similarity)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Toca para consultar el perfil",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ExpandedCandidateImage(
    candidate: MatchResult,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileImage(
                    reference = candidate.referenceImageBase64,
                    contentDescription = candidate.personName,
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Text(
                    candidate.personName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "Toca fuera de la imagen para cerrar.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
