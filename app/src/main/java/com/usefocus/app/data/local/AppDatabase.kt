package com.usefocus.app.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.usefocus.app.data.model.BlockRule

@Database(entities = [BlockRule::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rules(): BlockRuleDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE block_rules ADD COLUMN serviceId TEXT")
                }
            }
    }
}
