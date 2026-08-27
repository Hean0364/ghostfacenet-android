package com.example.ghostfacenet.data.db

import androidx.room.ColumnInfo
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
            entity = PerfilEntity::class,
            parentColumns = ["id"],
            childColumns = ["perfil_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("perfil_id"),
        Index("foto_id")
    ]
)
data class FaceEmbeddingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "perfil_id") val perfilId: Long,
    val embedding: ByteArray,
    @ColumnInfo(name = "source_image_path") val sourceImagePath: String,
    /**
     * Valor de perfiles.updateAt usado al generar este embedding.
     * Permite invalidarlo cuando el registro externo cambia su foto.
     */
    @ColumnInfo(name = "perfil_update_at") val perfilUpdateAt: Long,
    /**
     * ID de la foto adicional que produjo este embedding. Es null para la
     * foto principal almacenada en perfiles.foto_perfil.
     */
    @ColumnInfo(name = "foto_id") val fotoId: Long? = null,
    @ColumnInfo(name = "foto_update_at") val fotoUpdateAt: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEmbeddingEntity) return false
        return id == other.id &&
            perfilId == other.perfilId &&
            embedding.contentEquals(other.embedding) &&
            sourceImagePath == other.sourceImagePath &&
            perfilUpdateAt == other.perfilUpdateAt &&
            fotoId == other.fotoId &&
            fotoUpdateAt == other.fotoUpdateAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + perfilId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + sourceImagePath.hashCode()
        result = 31 * result + perfilUpdateAt.hashCode()
        result = 31 * result + (fotoId?.hashCode() ?: 0)
        result = 31 * result + (fotoUpdateAt?.hashCode() ?: 0)
        return result
    }
}
