package com.usefocus.app

import android.app.Application
import androidx.room.Room
import com.usefocus.app.data.local.AppDatabase
import com.usefocus.app.data.repository.BlockRuleRepository
import com.usefocus.app.data.website.WebsiteIconStore
import com.usefocus.app.vpn.ProtectionController

class BlockerApplication : Application() {
    val disclosures by lazy { com.usefocus.app.privacy.DisclosureConsentStore(this) }
    val adsConsent by lazy { com.usefocus.app.privacy.AdsConsentManager(this) }

    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "website-blocker.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    val repository by lazy { BlockRuleRepository(database.rules()) }
    val websiteIcons by lazy { WebsiteIconStore(this) }
    val protection by lazy { ProtectionController(this) }
}
