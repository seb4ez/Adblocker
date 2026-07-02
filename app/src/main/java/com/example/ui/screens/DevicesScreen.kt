package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.ui.theme.*
import com.example.ui.viewmodel.NetShieldViewModel

@Composable
fun DevicesScreen(
    viewModel: NetShieldViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    val localIp = remember { viewModel.getLocalIpAddress() ?: "192.168.1.1" }
    val subnet = remember(localIp) { localIp.substringBeforeLast(".") + ".0/24" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Subnet Scanner Control Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local Subnet Scanner",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target: $subnet",
                                color = CyberTeal,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isScanning) {
                            Button(
                                onClick = { viewModel.cancelScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("cancel_scan_button")
                            ) {
                                Text("Stop Scan", fontSize = 12.sp)
                            }
                        } else {
                            val infiniteTransition = rememberInfiniteTransition(label = "scan_spin")
                            val rotationAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotate"
                            )

                            Button(
                                onClick = { viewModel.scanLocalNetwork() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("start_scan_button")
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = SlateDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan LAN", color = SlateDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isScanning) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Scanning network reachability...",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${(scanProgress * 100).toInt()}%",
                                    color = EmeraldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = EmeraldPrimary,
                                trackColor = SlateLight
                            )
                        }
                    } else {
                        Text(
                            text = "Discovers connected Smart TVs, tablets, gaming consoles, & PCs to customize filtering access.",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "LAN DEVICE MANAGER (${devices.size})",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (devices.isEmpty()) {
            item {
                EmptyDevicesPlaceholder()
            }
        } else {
            items(devices) { device ->
                DeviceItemCard(
                    device = device,
                    onProfileChanged = { profile -> viewModel.updateDeviceProfile(device.ipAddress, profile) }
                )
            }
        }
    }
}

@Composable
fun DeviceItemCard(
    device: Device,
    onProfileChanged: (String) -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    val profiles = listOf("Ad-Block", "Family", "Work", "None")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.ipAddress}"),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // First Row: Icon, Device Details, Online Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device category icon
                val typeIcon = when (device.deviceType) {
                    "Smart TV" -> Icons.Default.Tv
                    "PC" -> Icons.Default.Computer
                    "Smartphone" -> Icons.Default.Smartphone
                    "Tablet" -> Icons.Default.TabletAndroid
                    else -> Icons.Default.SettingsCell
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberTeal.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = CyberTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = device.ipAddress,
                            color = TextGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "•",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = device.macAddress,
                            color = TextGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Online indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (device.isOnline) EmeraldPrimary else Color.Gray.copy(alpha = 0.5f))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SlateLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Second Row: Filtering Profile Action drop down trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FILTER PROFILE",
                        fontSize = 9.sp,
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = device.filteringProfile,
                        color = when (device.filteringProfile) {
                            "None" -> TextGray
                            "Family" -> CyberTeal
                            "Work" -> AlertOrange
                            else -> EmeraldPrimary
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    Button(
                        onClick = { expandedDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateLight),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Manage Access", fontSize = 11.sp, color = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextWhite, modifier = Modifier.size(14.dp))
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(SlateMedium).border(1.dp, SlateLight, RoundedCornerShape(4.dp))
                    ) {
                        profiles.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p, color = TextWhite, fontSize = 13.sp) },
                                onClick = {
                                    onProfileChanged(p)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDevicesPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PortableWifiOff,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No LAN devices discovered yet",
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Initiate a subnet reachability sweep above to discover other tablets, smartphones, and Smart TVs.",
            color = TextGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
