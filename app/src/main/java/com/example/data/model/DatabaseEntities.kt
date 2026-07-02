package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey val ipAddress: String,
    val macAddress: String,
    val name: String,
    val deviceType: String, // "Smartphone", "Tablet", "Smart TV", "PC", "Unknown"
    val filteringProfile: String, // "Ad-Block", "Family", "Work", "None"
    val isOnline: Boolean,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "dns_logs")
data class DnsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val domain: String,
    val sourceIp: String,
    val action: String, // "Allowed", "Blocked"
    val category: String // "Ad", "Tracker", "Social", "Standard", "Malware"
)

@Entity(tableName = "blocked_domains")
data class BlockedDomain(
    @PrimaryKey val domain: String,
    val isWhitelist: Boolean, // false = blacklist (block), true = whitelist (allow)
    val timestamp: Long = System.currentTimeMillis()
)
