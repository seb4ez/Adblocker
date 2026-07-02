package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DnsLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.NetShieldViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: NetShieldViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDnsRunning by viewModel.isDnsServerRunning.collectAsState()
    val isVpnRunning by viewModel.isVpnRunning.collectAsState()
    val totalQueries by viewModel.totalQueries.collectAsState()
    val blockedQueries by viewModel.blockedQueries.collectAsState()
    val blockedDomainsList by viewModel.blockedDomains.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()

    val localIp = remember { viewModel.getLocalIpAddress() ?: "127.0.0.1" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // 1. Sleek Cyber Shield Animated Banner
        item {
            CyberShieldBanner(localIp = localIp)
        }

        // 2. Active Services Control Cards
        item {
            Text(
                text = "SECURITY INTERFACES",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // DNS Server Toggle
                ServiceCard(
                    title = "LAN DNS Proxy",
                    subtitle = "Port 1053 • Bind 0.0.0.0",
                    isActive = isDnsRunning,
                    icon = Icons.Default.Router,
                    onToggle = { viewModel.toggleDnsServer() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dns_server_card")
                )

                // VPN On-Device Blocker
                ServiceCard(
                    title = "On-Device VPN",
                    subtitle = "Local DNS Intercept",
                    isActive = isVpnRunning,
                    icon = Icons.Default.Shield,
                    onToggle = { viewModel.toggleVpn(context) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vpn_service_card")
                )
            }
        }

        // 3. Real-Time Stats Grid
        item {
            Text(
                text = "FILTER METRICS",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total Queries",
                    value = totalQueries.toString(),
                    icon = Icons.Default.Analytics,
                    color = CyberTeal,
                    modifier = Modifier.weight(1f)
                )

                val blockRate = if (totalQueries > 0) {
                    (blockedQueries.toFloat() / totalQueries.toFloat() * 100).toInt()
                } else 0
                StatCard(
                    label = "Blocked Ads",
                    value = "$blockedQueries ($blockRate%)",
                    icon = Icons.Default.Block,
                    color = AlertRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Custom blocklist count info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ListAlt,
                        contentDescription = "Rules",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filter Database Active",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${blockedDomainsList.size} ad, tracker, & malware definitions active.",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 5. DNS Query Logs Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE FILTER STREAM",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                IconButton(
                    onClick = { viewModel.clearQueryLogs() },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Clear Logs",
                        tint = AlertOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 6. List of DNS logs
        if (recentLogs.isEmpty()) {
            item {
                EmptyLogsPlaceholder()
            }
        } else {
            items(recentLogs) { log ->
                DnsLogItem(log = log)
            }
        }
    }
}

@Composable
fun CyberShieldBanner(localIp: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SlateLight)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background grid visual elements drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridCount = 10
                val widthStep = size.width / gridCount
                val heightStep = size.height / gridCount

                for (i in 1 until gridCount) {
                    drawLine(
                        color = Color(0x0A00E676),
                        start = Offset(i * widthStep, 0f),
                        end = Offset(i * widthStep, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0x0A00E676),
                        start = Offset(0f, i * heightStep),
                        end = Offset(size.width, i * heightStep),
                        strokeWidth = 1f
                    )
                }
            }

            // Glow vector elements in center
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vector drawn shield
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(shieldScale)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 3.dp.toPx()
                        val w = size.width
                        val h = size.height

                        // Inner glowing radar arcs
                        drawCircle(
                            color = Color(0x1A00E676),
                            radius = w / 2f,
                            style = Stroke(width = strokeWidth / 2f)
                        )

                        // Cyber Shield Path
                        val shieldPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.15f, h * 0.2f)
                            quadraticTo(w * 0.5f, h * 0.12f, w * 0.85f, h * 0.2f)
                            lineTo(w * 0.85f, h * 0.55f)
                            quadraticTo(w * 0.85f, h * 0.85f, w * 0.5f, h * 0.95f)
                            quadraticTo(w * 0.15f, h * 0.85f, w * 0.15f, h * 0.55f)
                            close()
                        }

                        drawPath(
                            path = shieldPath,
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x3300E676), Color(0x0000E676)),
                                center = Offset(w / 2f, h / 2f)
                            )
                        )

                        drawPath(
                            path = shieldPath,
                            color = EmeraldPrimary,
                            style = Stroke(width = strokeWidth)
                        )

                        // Inner checkmark representing active block status
                        val checkPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.38f, h * 0.52f)
                            lineTo(w * 0.47f, h * 0.62f)
                            lineTo(w * 0.63f, h * 0.42f)
                        }
                        drawPath(
                            path = checkPath,
                            color = CyberTeal,
                            style = Stroke(width = strokeWidth * 1.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = "NetShield Active Protection",
                        fontSize = 17.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Subnet DNS Proxy Address:",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$localIp:1053",
                        fontSize = 18.sp,
                        color = CyberTeal,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Devices on LAN can use this DNS to block ads.",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_srv")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isActive) EmeraldPrimary.copy(alpha = 0.6f) else SlateLight)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) EmeraldPrimary else TextGray,
                    modifier = Modifier.size(24.dp)
                )

                // Running pulse indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) EmeraldPrimary.copy(alpha = pulseAlpha) else Color.Gray.copy(
                                alpha = 0.4f
                            )
                        )
                )
            }

            Column {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) AlertRed.copy(alpha = 0.2f) else EmeraldPrimary.copy(
                        alpha = 0.15f
                    ),
                    contentColor = if (isActive) AlertRed else EmeraldPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text(
                    text = if (isActive) "Deactivate" else "Activate",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateLight)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    color = TextGray,
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DnsLogItem(log: DnsLog) {
    val isBlocked = log.action == "Blocked"
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dns_log_item_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isBlocked) AlertRed.copy(alpha = 0.25f) else SlateLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action Badge indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isBlocked) AlertRed else EmeraldPrimary)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.domain,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Client: ${log.sourceIp}",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "•",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = log.category,
                        color = if (isBlocked) AlertOrange else CyberTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedTime,
                    color = TextGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isBlocked) AlertRed.copy(alpha = 0.15f) else EmeraldPrimary.copy(
                                alpha = 0.15f
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.action.uppercase(),
                        color = if (isBlocked) AlertRed else EmeraldPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLogsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PrivacyTip,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No DNS traffic resolved yet",
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Active filter logs will stream here in real-time.",
            color = TextGray,
            fontSize = 11.sp
        )
    }
}
