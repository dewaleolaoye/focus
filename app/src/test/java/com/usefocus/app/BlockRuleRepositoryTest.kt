package com.usefocus.app

import com.usefocus.app.data.local.BlockRuleDao
import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.data.repository.BlockRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockRuleRepositoryTest {
    @Test
    fun createEditEnableDisableAndDeletePersistThroughTheRepository() = runBlocking {
        val dao = FakeDao()
        val repository = BlockRuleRepository(dao)

        repository.save(rule(domain = " HTTPS://Goal.COM/path "))
        val created = dao.snapshot().single()
        assertNotEquals(0L, created.id)
        assertEquals("goal.com", created.domain)

        repository.save(created.copy(startMinute = 9 * 60, endMinute = 17 * 60))
        assertEquals(9 * 60, repository.find(created.id)?.startMinute)
        assertEquals(1, dao.snapshot().size)

        repository.setEnabled(created.id, false)
        assertFalse(repository.find(created.id)!!.enabled)
        repository.setEnabled(created.id, true)
        assertTrue(repository.find(created.id)!!.enabled)

        repository.delete(created.id)
        assertTrue(dao.snapshot().isEmpty())
    }

    @Test
    fun duplicateTargetsRemainSeparateSchedules() = runBlocking {
        val dao = FakeDao()
        val repository = BlockRuleRepository(dao)

        repository.save(rule(domain = "instagram.com", serviceId = "instagram"))
        repository.save(rule(domain = "instagram.com", serviceId = "instagram", start = 12 * 60))

        assertEquals(2, dao.snapshot().size)
        assertNotEquals(dao.snapshot()[0].id, dao.snapshot()[1].id)
    }

    @Test
    fun invalidIntervalsAndRepeatDaysAreRejectedBeforePersistence() {
        val repository = BlockRuleRepository(FakeDao())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.save(rule(start = 60, end = 60)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.save(rule(days = 0)) }
        }
    }

    private fun rule(
        domain: String = "example.com",
        serviceId: String? = null,
        start: Int = 8 * 60,
        end: Int = 9 * 60,
        days: Int = 127,
    ) = BlockRule(
        domain = domain,
        serviceId = serviceId,
        startMinute = start,
        endMinute = end,
        daysMask = days,
    )

    private class FakeDao : BlockRuleDao {
        private val state = MutableStateFlow<List<BlockRule>>(emptyList())
        private var nextId = 1L

        override fun observeRules(): Flow<List<BlockRule>> = state

        override suspend fun find(id: Long): BlockRule? = state.value.find { it.id == id }

        override suspend fun insert(rule: BlockRule): Long {
            val id = nextId++
            state.value = state.value + rule.copy(id = id)
            return id
        }

        override suspend fun update(rule: BlockRule): Int {
            if (state.value.none { it.id == rule.id }) return 0
            state.value = state.value.map { if (it.id == rule.id) rule else it }
            return 1
        }

        override suspend fun setEnabled(id: Long, enabled: Boolean) {
            state.value = state.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        }

        override suspend fun delete(id: Long) {
            state.value = state.value.filterNot { it.id == id }
        }

        fun snapshot(): List<BlockRule> = state.value
    }
}
