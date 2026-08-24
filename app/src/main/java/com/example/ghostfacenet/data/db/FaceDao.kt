package com.example.ghostfacenet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FaceDao {

    @Insert
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert
    suspend fun insertEmbedding(embedding: FaceEmbeddingEntity): Long

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getAllPeople(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :personId")
    suspend fun getPersonById(personId: Long): PersonEntity?

    @Query(
        """
        SELECT p.id AS personId, p.name AS personName, p.referenceImagePath AS referenceImagePath, e.embedding AS embedding
        FROM face_embeddings e
        INNER JOIN people p ON p.id = e.personId
        """
    )
    suspend fun getAllEmbeddingsWithPerson(): List<EmbeddingWithPerson>

    @Query("SELECT COUNT(*) FROM people")
    suspend fun countPeople(): Int

    @Query("SELECT * FROM face_embeddings")
    suspend fun getAllEmbeddings(): List<FaceEmbeddingEntity>

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()

    @Query("SELECT * FROM face_embeddings WHERE personId = :personId ORDER BY id ASC")
    suspend fun getEmbeddingsForPerson(personId: Long): List<FaceEmbeddingEntity>

    @Query("SELECT * FROM face_embeddings WHERE id = :embeddingId")
    suspend fun getEmbeddingById(embeddingId: Long): FaceEmbeddingEntity?

    @Query("DELETE FROM face_embeddings WHERE id = :embeddingId")
    suspend fun deleteEmbedding(embeddingId: Long)

    @Query("DELETE FROM people WHERE id = :personId")
    suspend fun deletePerson(personId: Long)

    @Query("UPDATE people SET name = :name WHERE id = :personId")
    suspend fun renamePerson(personId: Long, name: String)

    @Query("UPDATE people SET referenceImagePath = :path WHERE id = :personId")
    suspend fun updateReferenceImage(personId: Long, path: String)
}

/** Projection usada para el matching: no es una @Entity, solo el resultado de un JOIN. */
data class EmbeddingWithPerson(
    val personId: Long,
    val personName: String,
    val referenceImagePath: String,
    val embedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingWithPerson) return false
        return personId == other.personId &&
            personName == other.personName &&
            referenceImagePath == other.referenceImagePath &&
            embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = personId.hashCode()
        result = 31 * result + personName.hashCode()
        result = 31 * result + referenceImagePath.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
