package com.example.ghostfacenet.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un embedding facial (vector de 512 floats, serializado a bytes) asociado a
 * una foto de enrolamiento. Se guarda un registro por foto importada (no un
 * promedio), asi el matching puede quedarse con la mejor similitud por
 * persona entre varias poses/fotos.
 */
@Entity(
    tableName = "face_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class FaceEmbeddingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val embedding: ByteArray,
    val sourceImagePath: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEmbeddingEntity) return false
        return id == other.id &&
            personId == other.personId &&
            embedding.contentEquals(other.embedding) &&
            sourceImagePath == other.sourceImagePath
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + personId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + sourceImagePath.hashCode()
        return result
    }
}
