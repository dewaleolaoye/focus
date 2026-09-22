package com.usefocus.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.usefocus.app.data.local.AppDatabase
import com.usefocus.app.data.model.BlockRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockRuleDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun create() {
        db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    AppDatabase::class.java,
                )
                .build()
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun insertUpdateToggleDeleteAndObserve() = runBlocking {
        val dao = db.rules()
        assertTrue(dao.observeRules().first().isEmpty())
        val rule = BlockRule(domain = "x.com", startMinute = 1320, endMinute = 420, daysMask = 127)
        val id = dao.insert(rule)
        assertEquals(rule.copy(id = id), dao.observeRules().first().single())
        assertEquals(1, dao.update(rule.copy(id = id, domain = "example.com")))
        assertEquals("example.com", dao.find(id)!!.domain)
        dao.setEnabled(id, false)
        assertFalse(dao.observeRules().first().single().enabled)
        dao.delete(id)
        assertTrue(dao.observeRules().first().isEmpty())
    }

    @Test
    fun fileDatabaseSurvivesReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val id =
            first
                .rules()
                .insert(
                    BlockRule(domain = "x.com", startMinute = 1320, endMinute = 420, daysMask = 1)
                )
        first.close()
        val second = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            assertEquals(1, second.rules().find(id)!!.daysMask)
        } finally {
            second.close()
            context.deleteDatabase(name)
        }
    }
}
