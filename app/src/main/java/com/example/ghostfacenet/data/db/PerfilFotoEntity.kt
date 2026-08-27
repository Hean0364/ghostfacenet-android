package com.example.ghostfacenet.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Foto adicional de un perfil, cargada externamente en SQLite.
 *
 * La foto principal sigue viviendo en perfiles.foto_perfil. Esta tabla permite
 * agregar mas fotos de la misma persona sin crear perfiles duplicados.
 */
@Entity(
    tableName = "perfil_fotos",
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["id"],
            childColumns = ["perfil_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("perfil_id")]
)
data class PerfilFotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "perfil_id")
    val perfilId: Long,
    @ColumnInfo(name = "foto_base64")
    val fotoBase64: String,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updateAt")
    val updateAt: Long = createdAt
)
