package com.usefocus.app.domain

import com.usefocus.app.data.model.BlockRule

/**
 * User-facing services and the technical targets behind them.
 *
 * The UI stores only [id]. Domains and package variants stay centralized so they can evolve
 * without asking people to understand a service's infrastructure.
 */
data class ServiceProfile(
    val id: String,
    val name: String,
    val monogram: String,
    val domains: Set<String>,
    val androidPackages: Set<String>,
) {
    val primaryDomain: String
        get() = domains.first()
}

object ServiceCatalog {
    val popular =
        listOf(
            ServiceProfile(
                id = "instagram",
                name = "Instagram",
                monogram = "◎",
                domains = linkedSetOf(
                    "instagram.com",
                    "cdninstagram.com",
                    "ig.me",
                    "threads.net",
                ),
                androidPackages = setOf("com.instagram.android", "com.instagram.lite"),
            ),
            ServiceProfile(
                id = "facebook",
                name = "Facebook",
                monogram = "f",
                domains = linkedSetOf(
                    "facebook.com",
                    "fbcdn.net",
                    "facebook.net",
                    "fb.com",
                    "fb.me",
                    "fbsbx.com",
                    "m.facebook.com",
                ),
                androidPackages = setOf("com.facebook.katana", "com.facebook.lite"),
            ),
            ServiceProfile(
                id = "x",
                name = "X",
                monogram = "X",
                domains = linkedSetOf(
                    "x.com",
                    "twitter.com",
                    "twimg.com",
                    "t.co",
                    "api.twitter.com",
                    "api.x.com",
                    "ton.twitter.com",
                    "abs.twimg.com",
                    "pbs.twimg.com",
                    "pds.twitter.com",
                ),
                androidPackages = setOf("com.twitter.android"),
            ),
            ServiceProfile(
                id = "tiktok",
                name = "TikTok",
                monogram = "T",
                domains = linkedSetOf(
                    "tiktok.com",
                    "tiktokv.com",
                    "tiktokcdn.com",
                    "byteoversea.com",
                    "ibytedtos.com",
                    "musical.ly",
                ),
                androidPackages = setOf(
                    "com.zhiliaoapp.musically",
                    "com.zhiliaoapp.musically.go",
                    "com.ss.android.ugc.trill",
                ),
            ),
            ServiceProfile(
                id = "youtube",
                name = "YouTube",
                monogram = "▶",
                domains = linkedSetOf(
                    "youtube.com",
                    "youtu.be",
                    "googlevideo.com",
                    "ytimg.com",
                    "youtube-nocookie.com",
                    "youtubei.googleapis.com",
                    "yt3.ggpht.com",
                ),
                androidPackages = setOf(
                    "com.google.android.youtube",
                    "com.google.android.youtube.go",
                    "com.google.android.youtube.tv",
                ),
            ),
        )

    private val legacy =
        listOf(
            ServiceProfile(
                id = "whatsapp",
                name = "WhatsApp",
                monogram = "W",
                domains = linkedSetOf(
                    "whatsapp.com",
                    "whatsapp.net",
                    "wa.me",
                ),
                androidPackages = setOf("com.whatsapp", "com.whatsapp.w4b"),
            ),
        )

    fun find(id: String?) = popular.firstOrNull { it.id == id } ?: legacy.firstOrNull { it.id == id }

    fun domainsFor(rule: BlockRule): Set<String> =
        find(rule.serviceId)?.domains ?: if (rule.domain.isNotBlank()) setOf(rule.domain) else emptySet()

    fun displayName(rule: BlockRule): String =
        rule.appDisplayName ?: find(rule.serviceId)?.name ?: rule.packageName ?: rule.domain

    fun matches(hostname: String, rule: BlockRule): Boolean =
        domainsFor(rule).any { DomainNormalizer.matches(hostname, it) }

    fun matchesPackage(packageName: String, rule: BlockRule): Boolean =
        (rule.packageName != null && rule.packageName == packageName) ||
            find(rule.serviceId)?.androidPackages?.contains(packageName) == true
}
