package com.websiteblocker.app.domain

import com.websiteblocker.app.data.model.BlockRule

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
                domains = linkedSetOf("instagram.com", "cdninstagram.com"),
                androidPackages = setOf("com.instagram.android", "com.instagram.lite"),
            ),
            ServiceProfile(
                id = "whatsapp",
                name = "WhatsApp",
                monogram = "W",
                domains = linkedSetOf("whatsapp.com", "whatsapp.net"),
                androidPackages = setOf("com.whatsapp", "com.whatsapp.w4b"),
            ),
            ServiceProfile(
                id = "facebook",
                name = "Facebook",
                monogram = "f",
                domains = linkedSetOf("facebook.com", "fbcdn.net", "facebook.net"),
                androidPackages = setOf("com.facebook.katana", "com.facebook.lite"),
            ),
            ServiceProfile(
                id = "x",
                name = "X",
                monogram = "X",
                domains = linkedSetOf("x.com", "twitter.com", "twimg.com", "t.co"),
                androidPackages = setOf("com.twitter.android"),
            ),
            ServiceProfile(
                id = "youtube",
                name = "YouTube",
                monogram = "▶",
                domains =
                    linkedSetOf(
                        "youtube.com",
                        "youtu.be",
                        "googlevideo.com",
                        "ytimg.com",
                        "youtube-nocookie.com",
                        "youtubei.googleapis.com",
                    ),
                androidPackages =
                    setOf(
                        "com.google.android.youtube",
                        "com.google.android.youtube.go",
                        "com.google.android.youtube.tv",
                    ),
            ),
        )

    fun find(id: String?) = popular.firstOrNull { it.id == id }

    fun domainsFor(rule: BlockRule): Set<String> =
        find(rule.serviceId)?.domains ?: setOf(rule.domain)

    fun displayName(rule: BlockRule): String = find(rule.serviceId)?.name ?: rule.domain

    fun matches(hostname: String, rule: BlockRule): Boolean =
        domainsFor(rule).any { DomainNormalizer.matches(hostname, it) }

    fun matchesPackage(packageName: String, rule: BlockRule): Boolean =
        find(rule.serviceId)?.androidPackages?.contains(packageName) == true
}
