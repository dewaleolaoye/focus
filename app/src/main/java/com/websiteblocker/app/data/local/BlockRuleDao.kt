package com.websiteblocker.app.data.local

import androidx.room.*
import com.websiteblocker.app.data.model.BlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {
    @Query("SELECT * FROM block_rules ORDER BY domain, id")
    fun observeRules(): Flow<List<BlockRule>>

    @Query("SELECT * FROM block_rules WHERE id = :id") suspend fun find(id: Long): BlockRule?

    @Insert suspend fun insert(rule: BlockRule): Long

    @Update suspend fun update(rule: BlockRule): Int

    @Query("UPDATE block_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM block_rules WHERE id = :id") suspend fun delete(id: Long)
}
