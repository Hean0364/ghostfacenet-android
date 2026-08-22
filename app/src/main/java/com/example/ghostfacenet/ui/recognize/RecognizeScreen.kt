package com.example.ghostfacenet.ui.recognize

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostfacenet.GhostFaceNetApp
import com.example.ghostfacenet.data.RecognitionOutcome
import com.example.ghostfacenet.ui.ViewModelFactory
import com.example.ghostfacenet.ui.loadBitmapFromUri

@Composable
fun RecognizeScreen(app: GhostFaceNetApp, threshold: Float) {
    val context = LocalContext.current
    val viewModel: RecognizeViewModel = viewModel(factory = ViewModelFactory(app))
    val state by viewModel.state.collectAsState()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            previewBitmap = bitmap
            viewModel.recognize(bitmap, threshold)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) takePictureLauncher.launch(null) }

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
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("GhostFaceNet", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Reconocimiento facial 100% local",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

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
                if (granted) takePictureLauncher.launch(null)
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
                is RecognizeUiState.Result -> ResultCard(s.outcome)
            }
        }
    }
}

@Composable
private fun ResultCard(outcome: RecognitionOutcome) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (outcome) {
                is RecognitionOutcome.Match -> {
                    Text(
                        "✔ ${outcome.result.personName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Similitud: ${"%.3f".format(outcome.result.similarity)}")
                }
                is RecognitionOutcome.NoMatch ->
                    Text("Persona no reconocida (no supera el umbral configurado).")
                is RecognitionOutcome.NoFaceDetected ->
                    Text("No se detectó ningún rostro en la imagen.")
                is RecognitionOutcome.EmptyDatabase ->
                    Text("Todavía no hay personas importadas. Ve a la pestaña Importar.")
            }
        }
    }
}
