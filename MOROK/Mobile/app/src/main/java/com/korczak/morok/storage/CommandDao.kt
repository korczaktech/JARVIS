package com.korczak.morok.storage
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao interface CommandDao {
@Insert suspend fun insert(command: CommandEntity)
@Query("SELECT * FROM commands ORDER BY createdAtEpochMs DESC") fun observeAll(): Flow<List<CommandEntity>>
@Query("DELETE FROM commands") suspend fun clear()
}