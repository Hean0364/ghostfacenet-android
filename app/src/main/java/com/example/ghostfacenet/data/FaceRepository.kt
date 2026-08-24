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
import java.util.UUID

sealed class RecognitionOutcome {
    /** [alternatives]: siguientes candidatas mas cercanas, para detectar ambiguedad. */
    data class Match(val result: MatchResult, val alternatives: List<MatchResult> = emptyList()) : RecognitionOutcome()
    /** [closest]: candidatas mas cercanas que no llegaron al umbral (puede estar vacia si no hay nadie enrolado con embeddings validos). */
    data class NoMatch(val closest: List<MatchResult> = emptyList()) : RecognitionOutcome()
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

    suspend fun getPerson(personId: Long): PersonEntity? = withContext(Dispatchers.IO) {
        faceDao.getPersonById(personId)
    }

    suspend fun getPersonPhotos(personId: Long): List<FaceEmbeddingEntity> = withContext(Dispatchers.IO) {
        faceDao.getEmbeddingsForPerson(personId)
    }

    suspend fun hasEnrolledPeople(): Boolean = withContext(Dispatchers.IO) {
        faceDao.countPeople() > 0
    }

    suspend fun renamePerson(personId: Long, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) faceDao.renamePerson(personId, trimmed)
    }

    /** Elimina a la persona, sus embeddings (cascade) y sus fotos guardadas en disco. */
    suspend fun deletePerson(personId: Long) = withContext(Dispatchers.IO) {
        faceDao.getEmbeddingsForPerson(personId).forEach { deleteFileQuietly(it.sourceImagePath) }
        faceDao.getPersonById(personId)?.let { deleteFileQuietly(it.referenceImagePath) }
        faceDao.deletePerson(personId)
    }

    /** Elimina a todas las personas, todos sus embeddings y todas sus fotos guardadas en disco. */
    suspend fun deleteAllPeople() = withContext(Dispatchers.IO) {
        faceDao.getAllEmbeddings().forEach { deleteFileQuietly(it.sourceImagePath) }
        faceDao.getAllPeople().forEach { deleteFileQuietly(it.referenceImagePath) }
        faceDao.deleteAllPeople()
    }

    /**
     * Elimina una foto (embedding) de una persona. Si era la ultima foto que le
     * quedaba, elimina tambien a la persona. Devuelve true si la persona quedo
     * eliminada.
     */
    suspend fun deletePhoto(personId: Long, embeddingId: Long): Boolean = withContext(Dispatchers.IO) {
        val embedding = faceDao.getEmbeddingById(embeddingId) ?: return@withContext false
        deleteFileQuietly(embedding.sourceImagePath)
        faceDao.deleteEmbedding(embeddingId)

        val remaining = faceDao.getEmbeddingsForPerson(personId)
        if (remaining.isEmpty()) {
            faceDao.getPersonById(personId)?.let { deleteFileQuietly(it.referenceImagePath) }
            faceDao.deletePerson(personId)
            true
        } else {
            val person = faceDao.getPersonById(personId)
            if (person != null && person.referenceImagePath == embedding.sourceImagePath) {
                faceDao.updateReferenceImage(personId, remaining.first().sourceImagePath)
            }
            false
        }
    }

    /** Detecta, alinea y agrega una foto nueva a una persona ya existente. */
    suspend fun addPhotoToPerson(personId: Long, bitmap: Bitmap): Boolean =
        withContext(Dispatchers.Default) {
            val face = faceDetector.detectLargestFace(bitmap) ?: return@withContext false
            val aligned = FaceAligner.alignAndCrop(bitmap, face)
            val embedding = embeddingExtractor.extract(aligned)

            withContext(Dispatchers.IO) {
                val person = faceDao.getPersonById(personId)
                val savedPath = saveFaceImage(aligned, person?.name ?: "persona")
                faceDao.insertEmbedding(
                    FaceEmbeddingEntity(
                        personId = personId,
                        embedding = EmbeddingCodec.encode(embedding),
                        sourceImagePath = savedPath
                    )
                )
                if (person != null && faceDao.getEmbeddingsForPerson(personId).size == 1) {
                    faceDao.updateReferenceImage(personId, savedPath)
                }
            }
            true
        }

    /**
     * Registra una persona nueva a partir de fotos seleccionadas o tomadas con
     * la cámara. Las fotos sin rostro se omiten y no crean un registro vacío.
     */
    suspend fun importPhotosForPerson(
        personName: String,
        bitmaps: List<Bitmap>
    ): ImportSummary = withContext(Dispatchers.Default) {
        val trimmedName = personName.trim()
        if (trimmedName.isEmpty()) return@withContext ImportSummary(bitmaps.size, 0, 0)

        var imported = 0
        var skippedNoFace = 0
        var personId: Long? = null

        for (bitmap in bitmaps) {
            val face = faceDetector.detectLargestFace(bitmap)
            if (face == null) {
                skippedNoFace++
                continue
            }

            val aligned = FaceAligner.alignAndCrop(bitmap, face)
            val embedding = embeddingExtractor.extract(aligned)
            val savedImagePath = withContext(Dispatchers.IO) {
                saveFaceImage(aligned, trimmedName)
            }

            val currentPersonId = personId ?: withContext(Dispatchers.IO) {
                faceDao.insertPerson(
                    PersonEntity(
                        name = trimmedName,
                        referenceImagePath = savedImagePath
                    )
                )
            }.also { personId = it }

            withContext(Dispatchers.IO) {
                faceDao.insertEmbedding(
                    FaceEmbeddingEntity(
                        personId = currentPersonId,
                        embedding = EmbeddingCodec.encode(embedding),
                        sourceImagePath = savedImagePath
                    )
                )
            }
            imported++
        }

        ImportSummary(
            totalImages = bitmaps.size,
            imported = imported,
            skippedNoFace = skippedNoFace
        )
    }

    suspend fun recognize(bitmap: Bitmap, threshold: Float): RecognitionOutcome =
        withContext(Dispatchers.Default) {
            val face = faceDetector.detectLargestFace(bitmap)
                ?: return@withContext RecognitionOutcome.NoFaceDetected

            val aligned = FaceAligner.alignAndCrop(bitmap, face)
            val embedding = embeddingExtractor.extract(aligned)

            val candidates = withContext(Dispatchers.IO) { faceDao.getAllEmbeddingsWithPerson() }
            if (candidates.isEmpty()) return@withContext RecognitionOutcome.EmptyDatabase

            val ranked = FaceMatcher.findTopMatches(embedding, candidates, topN = 3)
            val best = ranked.firstOrNull()
            if (best != null && best.similarity >= threshold) {
                RecognitionOutcome.Match(best, alternatives = ranked.drop(1))
            } else {
                RecognitionOutcome.NoMatch(closest = ranked)
            }
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
                    val savedImagePath = withContext(Dispatchers.IO) { saveFaceImage(aligned, personName) }

                    val currentPersonId = personId ?: withContext(Dispatchers.IO) {
                        faceDao.insertPerson(
                            PersonEntity(name = personName, referenceImagePath = savedImagePath)
                        )
                    }.also { personId = it }

                    withContext(Dispatchers.IO) {
                        faceDao.insertEmbedding(
                            FaceEmbeddingEntity(
                                personId = currentPersonId,
                                embedding = EmbeddingCodec.encode(embedding),
                                sourceImagePath = savedImagePath
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

    private fun saveFaceImage(bitmap: Bitmap, personName: String): String {
        val dir = File(context.filesDir, "reference_faces").apply { mkdirs() }
        val safeName = personName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "${safeName}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        return file.absolutePath
    }

    private fun deleteFileQuietly(path: String) {
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // Ignorar: si el archivo ya no existe o no se puede borrar, no bloquea la operacion.
        }
    }
}
