package com.example.ghostfacenet.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object PerfilEstado {
    const val ACTIVO = "Activo"
    const val FALLECIDO = "Fallecido"

    val valores = setOf(ACTIVO, FALLECIDO)
}

@Entity(
    tableName = "perfiles",
    indices = [Index(value = ["estado"])]
)
data class PerfilEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "estado")
    val estado: String = PerfilEstado.ACTIVO,
    @ColumnInfo(name = "foto_perfil")
    val fotoPerfil: String,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updateAt")
    val updateAt: Long = createdAt
)
