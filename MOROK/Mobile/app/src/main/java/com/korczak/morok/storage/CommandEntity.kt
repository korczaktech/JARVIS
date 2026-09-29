package com.korczak.morok.storage
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "commands")
data class CommandEntity(@PrimaryKey val id: String, val rawText: String, val source: String, val createdAtEpochMs: Long)