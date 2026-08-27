package com.example.ghostfacenet.data

import android.content.Context
import android.graphics.Bitmap
import com.example.ghostfacenet.data.db.AppDatabase
import com.example.ghostfacenet.data.db.FaceEmbeddingEntity
import com.example.ghostfacenet.data.db.PerfilEntity
import com.example.ghostfacenet.data.db.PerfilFotoEntity
import com.example.ghostfacenet.ml.EmbeddingCodec
import com.example.ghostfacenet.ml.EmbeddingExtractor
import com.example.ghostfacenet.ml.FaceAligner
import com.example.ghostfacenet.ml.FaceDetector
import com.example.ghostfacenet.ml.FaceMatcher
import com.example.ghostfacenet.ml.MatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class RecognitionOutcome {
    data class Match(val result: MatchResult, val alternatives: List<MatchResult> = emptyList()) :
        RecognitionOutcome()

    data class NoMatch(val closest: List<MatchResult> = emptyList()) : RecognitionOutcome()
    data object NoFaceDetected : RecognitionOutcome()
    data object EmptyDatabase : RecognitionOutcome()
}

/**
 * Punto de acceso a Room y al reconocimiento facial local.
 * La única escritura interna prepara embeddings a partir de perfiles existentes.
 */
class FaceRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val faceDao = database.faceDao()
    private val faceDetector = FaceDetector()
    private val embeddingExtractor = EmbeddingExtractor(context)
    private val embeddingInitializationMutex = Mutex()

    fun observePeople(): Flow<List<PerfilEntity>> = faceDao.observePerfiles()

    fun observePerson(personId: Long): Flow<PerfilEntity?> =
        faceDao.observePerfilById(personId)

    fun observePersonPhotos(personId: Long): Flow<List<PerfilFotoEntity>> =
        faceDao.observeFotosForPerfil(personId)

    suspend fun getAllPeople(): List<PerfilEntity> = withContext(Dispatchers.IO) {
        faceDao.getAllPerfiles()
    }

    suspend fun getPerson(personId: Long): PerfilEntity? = withContext(Dispatchers.IO) {
        faceDao.getPerfilById(personId)
    }

    suspend fun getPersonPhotos(personId: Long): List<PerfilFotoEntity> =
        withContext(Dispatchers.IO) {
            faceDao.getFotosForPerfil(personId)
        }

    suspend fun hasEnrolledPeople(): Boolean = withContext(Dispatchers.IO) {
        faceDao.countPerfiles() > 0
    }

    /**
     * Genera los embeddings faltantes o desactualizados a partir de
     * perfiles.foto_perfil. La aplicación no ofrece controles de escritura;
     * esta es una preparación interna de los datos leídos desde la base.
     */
    suspend fun initializeEmbeddings() = embeddingInitializationMutex.withLock {
        val profiles = withContext(Dispatchers.IO) { faceDao.getAllPerfiles() }
        profiles.forEach { profile ->
            val additionalPhotos = withContext(Dispatchers.IO) {
                faceDao.getFotosForPerfil(profile.id)
            }
            val embeddings = withContext(Dispatchers.IO) {
                faceDao.getEmbeddingsForPerfil(profile.id)
            }

            // La foto principal es la de perfiles.foto_perfil. Las demás
            // llegan externamente desde perfil_fotos y comparten el perfil.
            val photos = buildList {
                add(EnrollmentPhoto(id = null, imageBase64 = profile.fotoPerfil, updateAt = null))
                additionalPhotos.forEach { photo ->
                    add(
                        EnrollmentPhoto(
                            id = photo.id,
                            imageBase64 = photo.fotoBase64,
                            updateAt = photo.updateAt
                        )
                    )
                }
            }

            val embeddingsAreCurrent =
                embeddings.size == photos.size &&
                    photos.all { photo ->
                        embeddings.any { embedding ->
                            embedding.fotoId == photo.id &&
                                embedding.perfilUpdateAt == profile.updateAt &&
                                embedding.fotoUpdateAt == photo.updateAt
                        }
                    }
            if (embeddingsAreCurrent) return@forEach

            val generatedEmbeddings = photos.mapNotNull { photo ->
                try {
                    val bitmap = Base64ImageCodec.decode(photo.imageBase64) ?: return@mapNotNull null
                    val face = faceDetector.detectLargestFace(bitmap) ?: return@mapNotNull null
                    val aligned = FaceAligner.alignAndCrop(bitmap, face)
                    val vector = embeddingExtractor.extract(aligned)
                    FaceEmbeddingEntity(
                        perfilId = profile.id,
                        embedding = EmbeddingCodec.encode(vector),
                        sourceImagePath = "",
                        perfilUpdateAt = profile.updateAt,
                        fotoId = photo.id,
                        fotoUpdateAt = photo.updateAt
                    )
                } catch (_: Exception) {
                    null
                }
            }

            if (generatedEmbeddings.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    faceDao.deleteEmbeddingsForPerfil(profile.id)
                    for (embedding in generatedEmbeddings) {
                        faceDao.insertEmbedding(embedding)
                    }
                }
            }
        }
    }

    suspend fun recognize(bitmap: Bitmap, threshold: Float): RecognitionOutcome =
        withContext(Dispatchers.Default) {
            initializeEmbeddings()
            val face = faceDetector.detectLargestFace(bitmap)
                ?: return@withContext RecognitionOutcome.NoFaceDetected
            val aligned = FaceAligner.alignAndCrop(bitmap, face)
            val embedding = embeddingExtractor.extract(aligned)
            val candidates = withContext(Dispatchers.IO) {
                faceDao.getAllEmbeddingsWithPerson()
            }
            if (candidates.isEmpty()) {
                return@withContext RecognitionOutcome.EmptyDatabase
            }

            val ranked = FaceMatcher.findTopMatches(embedding, candidates, topN = 5)
            val matches = ranked.filter { it.similarity >= threshold }
            val best = matches.firstOrNull()
            if (best != null) {
                RecognitionOutcome.Match(best, alternatives = matches.drop(1))
            } else {
                RecognitionOutcome.NoMatch()
            }
        }
}

private data class EnrollmentPhoto(
    val id: Long?,
    val imageBase64: String,
    val updateAt: Long?
)
