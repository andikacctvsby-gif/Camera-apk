package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val originalFilePath: String? = null,
    val title: String,
    val presetApplied: String = "Normal",
    val objectRemovedCount: Int = 0,
    val aiCaption: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
