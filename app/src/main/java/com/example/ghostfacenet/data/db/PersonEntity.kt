package com.example.ghostfacenet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val referenceImagePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
