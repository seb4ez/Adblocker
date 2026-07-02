package com.example.data.repository

import com.example.data.db.NetShieldDao
import com.example.data.model.BlockedDomain
import com.example.data.model.Device
import com.example.data.model.DnsLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NetShieldRepository(private val dao: NetShieldDao) {

    val allDevices: Flow<List<Device>> = dao.getAllDevices()
    val allBlockedDomains: Flow<List<BlockedDomain>> = dao.getAllBlockedDomains()
    val blockedCount: Flow<Int> = dao.getBlockedCount()
    val totalCount: Flow<Int> = dao.getTotalCount()

    fun getRecentLogs(limit: Int): Flow<List<DnsLog>> = dao.getRecentLogs(limit)

    suspend fun insertDevice(device: Device) = dao.insertDevice(device)

    suspend fun updateDeviceProfile(ip: String, profile: String) = dao.updateDeviceProfile(ip, profile)

    suspend fun deleteDevice(device: Device) = dao.deleteDevice(device)

    suspend fun getDeviceByIp(ip: String): Device? = dao.getDeviceByIp(ip)

    suspend fun insertLog(log: DnsLog) = dao.insertLog(log)

    suspend fun clearLogs() = dao.clearLogs()

    suspend fun addBlockedDomain(domain: String, isWhitelist: Boolean) {
        dao.insertBlockedDomain(BlockedDomain(domain = domain.trim().lowercase(), isWhitelist = isWhitelist))
    }

    suspend fun deleteBlockedDomain(domain: String) {
        dao.deleteBlockedDomain(domain.trim().lowercase())
    }

    suspend fun isDomainBlocked(domain: String, sourceIp: String? = null): Boolean {
        val cleanDomain = domain.trim().lowercase()

        // 1. Check custom user settings (whitelist overrides blacklist)
        val customRule = dao.getBlockedDomain(cleanDomain)
        if (customRule != null) {
            return !customRule.isWhitelist
        }

        // Also check if any parent/subdomain is configured (e.g. if we block "doubleclick.net", we should block "test.doubleclick.net")
        val parts = cleanDomain.split(".")
        if (parts.size > 2) {
            for (i in 1 until parts.size - 1) {
                val parentDomain = parts.subList(i, parts.size).joinToString(".")
                val parentRule = dao.getBlockedDomain(parentDomain)
                if (parentRule != null) {
                    return !parentRule.isWhitelist
                }
            }
        }

        // 2. Check Device-specific profiling. If device is in "Family" filter, block social/adult sites, etc.
        if (sourceIp != null) {
            val device = dao.getDeviceByIp(sourceIp)
            if (device != null) {
                when (device.filteringProfile) {
                    "None" -> return false // "None" bypasses default blocklist entirely!
                    "Work" -> {
                        // In Work mode, we block distraction sites (social media, streaming)
                        if (isSocialOrDistraction(cleanDomain)) return true
                    }
                    "Family" -> {
                        // In Family mode, we block adult/gambling or strict tracker/ads
                        if (isAdultOrGambling(cleanDomain)) return true
                    }
                }
            }
        }

        // 3. Fallback to default blocklist
        return isDefaultBlocked(cleanDomain)
    }

    // Initialize default domains in database if empty
    suspend fun initializeDefaultDomainsIfNeeded() {
        val currentList = dao.getAllBlockedDomains().first()
        if (currentList.isEmpty()) {
            val defaultBlocks = listOf(
                // Common Ads
                "doubleclick.net" to false,
                "googleads.g.doubleclick.net" to false,
                "adservice.google.com" to false,
                "pagead2.googlesyndication.com" to false,
                "adaway.org" to false,
                "applovin.com" to false,
                "applvn.com" to false,
                "unityads.unity3d.com" to false,
                "chartboost.com" to false,
                "flurry.com" to false,
                "ads.tiktok.com" to false,
                "ads.facebook.com" to false,
                "ads.twitter.com" to false,
                "adcolony.com" to false,
                "mopub.com" to false,
                "vungle.com" to false,
                "admob.com" to false,
                "inmobi.com" to false,
                "adnxs.com" to false,
                
                // Trackers / Telemetry
                "telemetry.microsoft.com" to false,
                "telemetry.office.com" to false,
                "analytics.google.com" to false,
                "google-analytics.com" to false,
                "hotjar.com" to false,
                "mixpanel.com" to false,
                "amplitude.com" to false,
                "segment.io" to false,
                "crashlytics.com" to false,
                "stats.g.doubleclick.net" to false,
                "scorecardresearch.com" to false,

                // Malware / Phishing examples
                "malwaredomain.test" to false,
                "phishing-example-alert.com" to false,

                // Standard wholesome whitelisted sites for easy reference/assurance
                "google.com" to true,
                "wikipedia.org" to true,
                "github.com" to true,
                "android.com" to true,
                "stackoverflow.com" to true
            )
            for ((domain, isWhitelist) in defaultBlocks) {
                dao.insertBlockedDomain(BlockedDomain(domain, isWhitelist))
            }
        }
    }

    private fun isDefaultBlocked(domain: String): Boolean {
        val blockKeywords = listOf(
            "adserver", "adsystem", "adtracker", "adclick", "ads.", "telemetry.", "analytics.", "tracking."
        )
        for (keyword in blockKeywords) {
            if (domain.contains(keyword)) {
                return true
            }
        }
        return false
    }

    private fun isSocialOrDistraction(domain: String): Boolean {
        val distractionDomains = listOf(
            "facebook.com", "instagram.com", "tiktok.com", "twitter.com", "x.com",
            "reddit.com", "netflix.com", "twitch.tv", "youtube.com", "youtu.be"
        )
        return distractionDomains.any { domain == it || domain.endsWith(".$it") }
    }

    private fun isAdultOrGambling(domain: String): Boolean {
        val adultGamblingDomains = listOf(
            "gambling.com", "bet365.com", "pokerstars.com", "casino.com", "betonline.ag",
            "adultfriendfinder.com", "pornhub.com", "xvideos.com", "redtube.com", "xnxx.com"
        )
        return adultGamblingDomains.any { domain == it || domain.endsWith(".$it") }
    }
}
