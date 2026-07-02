package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockedDomain
import com.example.data.model.Device
import com.example.data.model.DnsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface NetShieldDao {

    // --- Devices ---
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<Device>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)

    @Query("UPDATE devices SET filteringProfile = :profile WHERE ipAddress = :ipAddress")
    suspend fun updateDeviceProfile(ipAddress: String, profile: String)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("SELECT * FROM devices WHERE ipAddress = :ipAddress LIMIT 1")
    suspend fun getDeviceByIp(ipAddress: String): Device?


    // --- DNS Logs ---
    @Query("SELECT * FROM dns_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<DnsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DnsLog)

    @Query("DELETE FROM dns_logs")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM dns_logs WHERE action = 'Blocked'")
    fun getBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dns_logs")
    fun getTotalCount(): Flow<Int>


    // --- Blocked/Allowed Domains ---
    @Query("SELECT * FROM blocked_domains ORDER BY timestamp DESC")
    fun getAllBlockedDomains(): Flow<List<BlockedDomain>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedDomain(blockedDomain: BlockedDomain)

    @Query("DELETE FROM blocked_domains WHERE domain = :domain")
    suspend fun deleteBlockedDomain(domain: String)

    @Query("SELECT * FROM blocked_domains WHERE domain = :domain LIMIT 1")
    suspend fun getBlockedDomain(domain: String): BlockedDomain?
}
