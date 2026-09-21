package com.websiteblocker.app

import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.domain.ServiceCatalog
import org.junit.Assert.*
import org.junit.Test

class ServiceCatalogTest {
    private fun rule(serviceId: String? = null, domain: String = "example.com") =
        BlockRule(
            domain = domain,
            serviceId = serviceId,
            startMinute = 0,
            endMinute = 1,
            daysMask = 127,
        )

    @Test
    fun popularProfilesHaveStableUniqueTargets() {
        assertEquals(6, ServiceCatalog.popular.map { it.id }.distinct().size)
        ServiceCatalog.popular.forEach { profile ->
            assertTrue(profile.name.isNotBlank())
            assertTrue(profile.domains.isNotEmpty())
            profile.domains.forEach { assertEquals(it, com.websiteblocker.app.domain.DomainNormalizer.normalize(it)) }
        }
    }

    @Test
    fun serviceRuleMatchesEveryBundledDomain() {
        val instagram = rule("instagram")
        assertTrue(ServiceCatalog.matches("i.instagram.com", instagram))
        assertTrue(ServiceCatalog.matches("scontent.cdninstagram.com", instagram))
        assertFalse(ServiceCatalog.matches("example.com", instagram))
    }

    @Test
    fun youtubeRuleMatchesWebAndApiHosts() {
        val youtube = rule("youtube")
        assertTrue(ServiceCatalog.matches("www.youtube.com", youtube))
        assertTrue(ServiceCatalog.matches("rr1---sn.googlevideo.com", youtube))
        assertTrue(ServiceCatalog.matches("youtubei.googleapis.com", youtube))
        assertFalse(ServiceCatalog.matches("google.com", youtube))
    }

    @Test
    fun customRuleOnlyMatchesItsWebsite() {
        val custom = rule(domain = "example.com")
        assertTrue(ServiceCatalog.matches("www.example.com", custom))
        assertFalse(ServiceCatalog.matches("example.org", custom))
        assertEquals("example.com", ServiceCatalog.displayName(custom))
    }

    @Test
    fun serviceRuleMatchesKnownAppVariants() {
        val whatsapp = rule("whatsapp")
        assertTrue(ServiceCatalog.matchesPackage("com.whatsapp", whatsapp))
        assertTrue(ServiceCatalog.matchesPackage("com.whatsapp.w4b", whatsapp))
        assertFalse(ServiceCatalog.matchesPackage("com.instagram.android", whatsapp))
        assertFalse(ServiceCatalog.matchesPackage("com.whatsapp", rule()))

        val youtube = rule("youtube")
        assertTrue(ServiceCatalog.matchesPackage("com.google.android.youtube", youtube))
        assertFalse(ServiceCatalog.matchesPackage("com.google.android.apps.youtube.music", youtube))
    }
}
