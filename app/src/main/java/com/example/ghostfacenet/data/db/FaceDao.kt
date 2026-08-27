package com.example.ghostfacenet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {

    @Insert
    suspend fun insertEmbedding(embedding: FaceEmbeddingEntity): Long

    @Query("SELECT * FROM perfiles ORDER BY nombre COLLATE NOCASE ASC, id ASC")
    fun observePerfiles(): Flow<List<PerfilEntity>>

    @Query("SELECT * FROM perfiles ORDER BY nombre COLLATE NOCASE ASC, id ASC")
    suspend fun getAllPerfiles(): List<PerfilEntity>

    @Query("SELECT * FROM perfiles WHERE id = :perfilId")
    suspend fun getPerfilById(perfilId: Long): PerfilEntity?

    @Query("SELECT * FROM perfiles WHERE id = :perfilId")
    fun observePerfilById(perfilId: Long): Flow<PerfilEntity?>

    @Query("SELECT * FROM perfil_fotos WHERE perfil_id = :perfilId ORDER BY id ASC")
    suspend fun getFotosForPerfil(perfilId: Long): List<PerfilFotoEntity>

    @Query("SELECT * FROM perfil_fotos WHERE perfil_id = :perfilId ORDER BY id ASC")
    fun observeFotosForPerfil(perfilId: Long): Flow<List<PerfilFotoEntity>>

    @Query("SELECT * FROM face_embeddings WHERE perfil_id = :perfilId ORDER BY id ASC")
    suspend fun getEmbeddingsForPerfil(perfilId: Long): List<FaceEmbeddingEntity>

    @Query("SELECT * FROM face_embeddings WHERE perfil_id = :perfilId ORDER BY id ASC")
    fun observeEmbeddingsForPerfil(perfilId: Long): Flow<List<FaceEmbeddingEntity>>

    @Query("DELETE FROM face_embeddings WHERE perfil_id = :perfilId")
    suspend fun deleteEmbeddingsForPerfil(perfilId: Long)

    @Query(
        """
        SELECT p.id AS perfilId, p.nombre AS personName, p.foto_perfil AS referenceImageBase64, e.embedding AS embedding
        FROM face_embeddings e
        INNER JOIN perfiles p ON p.id = e.perfil_id
        WHERE p.estado = 'Activo'
          AND e.perfil_update_at = p.updateAt
        """
    )
    suspend fun getAllEmbeddingsWithPerson(): List<EmbeddingWithPerson>

    @Query("SELECT COUNT(*) FROM perfiles")
    suspend fun countPerfiles(): Int
}

/** Projection usada para el matching: no es una @Entity, solo el resultado de un JOIN. */
data class EmbeddingWithPerson(
    val perfilId: Long,
    val personName: String,
    val referenceImageBase64: String,
    val embedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingWithPerson) return false
        return perfilId == other.perfilId &&
            personName == other.personName &&
            referenceImageBase64 == other.referenceImageBase64 &&
            embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = perfilId.hashCode()
        result = 31 * result + personName.hashCode()
        result = 31 * result + referenceImageBase64.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
