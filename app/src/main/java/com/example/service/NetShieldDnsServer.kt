package com.example.service

import android.util.Log
import com.example.data.model.DnsLog
import com.example.data.repository.NetShieldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class NetShieldDnsServer(
    private val repository: NetShieldRepository,
    private val serverPort: Int = 1053 // Default non-privileged DNS port
) {
    private val tag = "NetShieldDnsServer"
    private var socket: DatagramSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    fun start() {
        if (_isServerRunning.value) return
        _isServerRunning.value = true

        serverJob = scope.launch {
            try {
                Log.d(tag, "Starting DNS Server on port $serverPort...")
                socket = DatagramSocket(serverPort, InetAddress.getByName("0.0.0.0")).apply {
                    soTimeout = 3000 // 3 seconds timeout to check running state periodically
                }

                val buffer = ByteArray(1024)
                while (_isServerRunning.value) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket?.receive(packet)
                        handleDnsQuery(packet)
                    } catch (e: SocketTimeoutException) {
                        // Timeout hit, loop again to check if server was stopped
                    } catch (e: Exception) {
                        if (_isServerRunning.value) {
                            Log.e(tag, "Error receiving packet", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to start DNS Server socket", e)
                _isServerRunning.value = false
            } finally {
                stopSocket()
            }
        }
    }

    fun stop() {
        _isServerRunning.value = false
        stopSocket()
        serverJob?.cancel()
        serverJob = null
        Log.d(tag, "DNS Server stopped.")
    }

    private fun stopSocket() {
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null
    }

    private fun handleDnsQuery(queryPacket: DatagramPacket) {
        val clientIp = queryPacket.address.hostAddress ?: "127.0.0.1"
        val data = queryPacket.data.copyOf(queryPacket.length)

        if (data.size < 12) return // Invalid DNS header

        scope.launch {
            try {
                val txId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                val (domain, questionEnd) = parseDnsQuestion(data)

                if (domain.isNotEmpty()) {
                    val isBlocked = repository.isDomainBlocked(domain, clientIp)
                    Log.d(tag, "DNS Request from $clientIp: $domain (Blocked: $isBlocked)")

                    // Log in DB
                    val category = getDomainCategory(domain)
                    repository.insertLog(
                        DnsLog(
                            domain = domain,
                            sourceIp = clientIp,
                            action = if (isBlocked) "Blocked" else "Allowed",
                            category = if (isBlocked) category else "Standard"
                        )
                    )

                    // Also auto-discover local device
                    autoDiscoverDevice(clientIp)

                    if (isBlocked) {
                        // Respond with 0.0.0.0
                        val response = buildBlockedResponse(data, txId, questionEnd)
                        val replyPacket = DatagramPacket(
                            response,
                            response.size,
                            queryPacket.address,
                            queryPacket.port
                        )
                        socket?.send(replyPacket)
                    } else {
                        // Forward query to Google Public DNS (8.8.8.8) or Cloudflare (1.1.1.1)
                        forwardDnsQuery(data, queryPacket)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error handling DNS query", e)
            }
        }
    }

    private fun forwardDnsQuery(queryData: ByteArray, clientPacket: DatagramPacket) {
        try {
            val forwardSocket = DatagramSocket()
            forwardSocket.soTimeout = 2000 // 2 seconds resolve timeout

            val upstreamDns = InetAddress.getByName("1.1.1.1") // Secure upstream Cloudflare DNS
            val forwardPacket = DatagramPacket(queryData, queryData.size, upstreamDns, 53)
            forwardSocket.send(forwardPacket)

            val replyBuffer = ByteArray(1024)
            val replyPacket = DatagramPacket(replyBuffer, replyBuffer.size)
            forwardSocket.receive(replyPacket)

            val dnsResponseBytes = replyPacket.data.copyOf(replyPacket.length)

            val clientReplyPacket = DatagramPacket(
                dnsResponseBytes,
                dnsResponseBytes.size,
                clientPacket.address,
                clientPacket.port
            )
            socket?.send(clientReplyPacket)
            forwardSocket.close()
        } catch (e: Exception) {
            Log.e(tag, "Error forwarding DNS query to upstream resolver", e)
        }
    }

    private fun parseDnsQuestion(data: ByteArray): Pair<String, Int> {
        var index = 12 // DNS Header is 12 bytes
        val domainParts = mutableListOf<String>()

        while (index < data.size) {
            val labelLength = data[index].toInt() and 0xFF
            if (labelLength == 0) {
                index++
                break
            }
            if (index + 1 + labelLength > data.size) return "" to index

            val labelBytes = data.copyOfRange(index + 1, index + 1 + labelLength)
            domainParts.add(String(labelBytes))
            index += 1 + labelLength
        }
        // index now points to QTYPE (2 bytes) + QCLASS (2 bytes)
        val questionEnd = index + 4
        return domainParts.joinToString(".") to questionEnd
    }

    private fun buildBlockedResponse(queryData: ByteArray, txId: Int, questionEnd: Int): ByteArray {
        // DNS Header (12 bytes)
        val response = ByteArray(questionEnd + 16) // Header + original question + A Answer Record

        // Copy original Transaction ID
        response[0] = (txId ushr 8).toByte()
        response[1] = (txId and 0xFF).toByte()

        // Set response flags: Standard response, No error, Recursion desired + available
        // Flags: 0x8180
        response[2] = 0x81.toByte()
        response[3] = 0x80.toByte()

        // Question Count = 1
        response[4] = 0
        response[5] = 1

        // Answer Count = 1
        response[6] = 0
        response[7] = 1

        // Authority / Additional Count = 0
        response[8] = 0
        response[9] = 0
        response[10] = 0
        response[11] = 0

        // Copy original Question section (from byte 12 up to questionEnd)
        System.arraycopy(queryData, 12, response, 12, questionEnd - 12)

        // Append Answer Resource Record (A-Record pointing to 0.0.0.0)
        var offset = questionEnd

        // Name pointer back to Domain Name in Question section (0xC00C is a standard DNS pointer)
        response[offset++] = 0xC0.toByte()
        response[offset++] = 0x0C.toByte()

        // Type: A record (0x0001)
        response[offset++] = 0x00.toByte()
        response[offset++] = 0x01.toByte()

        // Class: IN (0x0001)
        response[offset++] = 0x00.toByte()
        response[offset++] = 0x01.toByte()

        // TTL: 3600 seconds (0x00000E10)
        response[offset++] = 0x00.toByte()
        response[offset++] = 0x00.toByte()
        response[offset++] = 0x0E.toByte()
        response[offset++] = 0x10.toByte()

        // RDLength: 4 bytes (for IPv4 address size)
        response[offset++] = 0x00.toByte()
        response[offset++] = 0x04.toByte()

        // RData: IP Address 0.0.0.0
        response[offset++] = 0.toByte()
        response[offset++] = 0.toByte()
        response[offset++] = 0.toByte()
        response[offset++] = 0.toByte()

        return response.copyOf(offset)
    }

    private fun getDomainCategory(domain: String): String {
        return when {
            domain.contains("analytics") || domain.contains("telemetry") -> "Tracker"
            domain.contains("facebook") || domain.contains("tiktok") || domain.contains("instagram") -> "Social"
            domain.contains("malware") || domain.contains("phishing") -> "Malware"
            else -> "Ad"
        }
    }

    private suspend fun autoDiscoverDevice(ip: String) {
        if (ip == "127.0.0.1" || ip == "0.0.0.0") return
        val existing = repository.getDeviceByIp(ip)
        if (existing == null) {
            val deviceType = when {
                ip.endsWith(".1") -> "Unknown" // Router usually
                ip.endsWith(".2") || ip.endsWith(".15") -> "Smart TV"
                ip.endsWith(".10") || ip.endsWith(".20") -> "PC"
                else -> "Smartphone"
            }
            val name = when (deviceType) {
                "Smart TV" -> "Smart TV (Network)"
                "PC" -> "Desktop PC"
                else -> "Mobile Device ($ip)"
            }
            repository.insertDevice(
                com.example.data.model.Device(
                    ipAddress = ip,
                    macAddress = generateRandomMac(ip),
                    name = name,
                    deviceType = deviceType,
                    filteringProfile = "Ad-Block",
                    isOnline = true
                )
            )
        } else {
            // Update online status
            if (!existing.isOnline) {
                repository.insertDevice(existing.copy(isOnline = true, lastSeen = System.currentTimeMillis()))
            }
        }
    }

    private fun generateRandomMac(ip: String): String {
        val hash = ip.hashCode().coerceAtLeast(0)
        val part1 = (hash and 0xFF).toString(16).padStart(2, '0')
        val part2 = ((hash ushr 8) and 0xFF).toString(16).padStart(2, '0')
        return "74:AC:5F:8B:$part1:$part2".uppercase()
    }
}
