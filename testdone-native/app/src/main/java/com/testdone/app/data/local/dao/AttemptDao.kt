package com.testdone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttempt(attempt: com.testdone.app.data.local.entity.AttemptEntity)

    @Query("SELECT * FROM attempts ORDER BY COALESCE(completedAt, startedAt) DESC LIMIT 100")
    fun observeAttempts(): Flow<List<com.testdone.app.data.local.entity.AttemptEntity>>

    @Query("SELECT * FROM attempts ORDER BY COALESCE(completedAt, startedAt) DESC LIMIT 100")
    suspend fun allAttempts(): List<com.testdone.app.data.local.entity.AttemptEntity>

    @Query("SELECT * FROM attempts WHERE attemptId = :attemptId")
    suspend fun byId(attemptId: String): com.testdone.app.data.local.entity.AttemptEntity?

    @Query("SELECT * FROM attempts WHERE attemptId IN (:ids)")
    suspend fun byIds(ids: List<String>): List<com.testdone.app.data.local.entity.AttemptEntity>

    @Query("SELECT * FROM attempts WHERE synced = 0")
    suspend fun unsynced(): List<com.testdone.app.data.local.entity.AttemptEntity>

    @Query("UPDATE attempts SET synced = 1 WHERE attemptId IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM attempts")
    suspend fun count(): Int

    /** Cloud merge: keep cloud rows authoritative, append local-only rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mergeCloud(attempts: List<com.testdone.app.data.local.entity.AttemptEntity>)

    @Query("DELETE FROM attempts")
    suspend fun clearAll()
}
