package com.example.ghostfacenet.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.ghostfacenet.data.db.AppDatabase
import com.example.ghostfacenet.data.db.FaceEmbeddingEntity
import com.example.ghostfacenet.data.db.PersonEntity
import com.example.ghostfacenet.ml.EmbeddingCodec
import com.example.ghostfacenet.ml.EmbeddingExtractor
import com.example.ghostfacenet.ml.FaceAligner
import com.example.ghostfacenet.ml.FaceDetector
import com.example.ghostfacenet.ml.FaceMatcher
import com.example.ghostfacenet.ml.MatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class RecognitionOutcome {
    data class Match(val result: MatchResult) : RecognitionOutcome()
    data object NoMatch : RecognitionOutcome()
    data object NoFaceDetected : RecognitionOutcome()
    data object EmptyDatabase : RecognitionOutcome()
}

data class ImportSummary(
    val totalImages: Int,
    val imported: Int,
    val skippedNoFace: Int
)

/**
 * Punto unico de acceso a: deteccion de rostro, extraccion de embeddings y
 * persistencia local (Room/SQLite). Usado tanto por el flujo de
 * reconocimiento como por el de importacion/enrolamiento.
 */
class FaceRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val faceDao = database.faceDao()
    private val faceDetector = FaceDetector()
    private val embeddingExtractor = EmbeddingExtractor(context)

    suspend fun getAllPeople(): List<PersonEntity> = withContext(Dispatchers.IO) {
        faceDao.getAllPeople()
    }

    suspend fun hasEnrolledPeople(): Boolean = withContext(Dispatchers.IO) {
        faceDao.countPeople() > 0
    }

    suspend fun recognize(bitmap: Bitmap, threshold: Float): RecognitionOutcome =
        withContext(Dispatchers.Default) {
            val face = faceDetector.detectLargestFace(bitmap)
                ?: return@withContext RecognitionOutcome.NoFaceDetected

            val aligned = FaceAligner.alignAndCrop(bitmap, face)
            val embedding = embeddingExtractor.extract(aligned)

            val candidates = withContext(Dispatchers.IO) { faceDao.getAllEmbeddingsWithPerson() }
            if (candidates.isEmpty()) return@withContext RecognitionOutcome.EmptyDatabase

            val match = FaceMatcher.findBestMatch(embedding, candidates, threshold)
            if (match != null) RecognitionOutcome.Match(match) else RecognitionOutcome.NoMatch
        }

    /**
     * Recorre una carpeta elegida por el usuario (Storage Access Framework).
     * Convencion: una subcarpeta por persona; si no hay subcarpetas, cada
     * archivo se trata como una persona distinta usando su nombre de archivo.
     */
    suspend fun importPhotosFromTree(treeUri: Uri): ImportSummary =
        withContext(Dispatchers.Default) {
            var imported = 0
            var skippedNoFace = 0
            var totalImages = 0

            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext ImportSummary(0, 0, 0)

            val personFolders = root.listFiles().filter { it.isDirectory }

            val entries: List<Pair<String, List<DocumentFile>>> = if (personFolders.isNotEmpty()) {
                personFolders.map { folder ->
                    (folder.name ?: "Persona") to folder.listFiles()
                        .filter { it.isFile && it.type?.startsWith("image/") == true }
                }
            } else {
                root.listFiles()
                    .filter { it.isFile && it.type?.startsWith("image/") == true }
                    .map { file -> (file.name?.substringBeforeLast('.') ?: "Persona") to listOf(file) }
            }

            for ((personName, files) in entries) {
                var personId: Long? = null

                for (file in files) {
                    totalImages++
                    val bitmap = loadBitmap(file.uri) ?: continue
                    val face = faceDetector.detectLargestFace(bitmap)
                    if (face == null) {
                        skippedNoFace++
                        continue
                    }

                    val aligned = FaceAligner.alignAndCrop(bitmap, face)
                    val embedding = embeddingExtractor.extract(aligned)

                    val currentPersonId = personId ?: withContext(Dispatchers.IO) {
                        val savedImagePath = saveReferenceImage(aligned, personName)
                        faceDao.insertPerson(
                            PersonEntity(name = personName, referenceImagePath = savedImagePath)
                        )
                    }.also { personId = it }

                    withContext(Dispatchers.IO) {
                        faceDao.insertEmbedding(
                            FaceEmbeddingEntity(
                                personId = currentPersonId,
                                embedding = EmbeddingCodec.encode(embedding),
                                sourceImagePath = file.uri.toString()
                            )
                        )
                    }
                    imported++
                }
            }

            ImportSummary(totalImages = totalImages, imported = imported, skippedNoFace = skippedNoFace)
        }

    private fun loadBitmap(uri: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }

    private fun saveReferenceImage(bitmap: Bitmap, personName: String): String {
        val dir = File(context.filesDir, "reference_faces").apply { mkdirs() }
        val safeName = personName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "${safeName}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        return file.absolutePath
    }
}
