package com.websiteblocker.app

import android.app.Application
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.websiteblocker.app.data.local.AppDatabase
import com.websiteblocker.app.data.repository.BlockRuleRepository
import com.websiteblocker.app.data.website.WebsiteIconStore
import com.websiteblocker.app.vpn.ProtectionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlockerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@BlockerApplication) {}
        }
    }

    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "website-blocker.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    val repository by lazy { BlockRuleRepository(database.rules()) }
    val websiteIcons by lazy { WebsiteIconStore(this) }
    val protection by lazy { ProtectionController(this) }
}
