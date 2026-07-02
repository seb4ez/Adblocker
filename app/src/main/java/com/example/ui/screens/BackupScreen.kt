package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.NetShieldViewModel

@Composable
fun BackupScreen(
    viewModel: NetShieldViewModel,
    modifier: Modifier = Modifier
) {
    val backupStatus by viewModel.backupStatus.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val localIp = remember { viewModel.getLocalIpAddress() ?: "192.168.1.104" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. Google Drive Cloud Backup Module
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = CyberTeal)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Drive Backups",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "dejameiniciars@gmail.com",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONNECTED",
                                color = EmeraldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Store encrypted, user-owned configuration files directly on your secure Google Drive. Keep lists in sync across devices.",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.performGoogleDriveBackup() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(40.dp)
                                .testTag("drive_backup_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = SlateDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup", color = SlateDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.restoreFromGoogleDrive() },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("drive_restore_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore", color = TextWhite, fontSize = 12.sp)
                        }
                    }

                    AnimatedVisibility(visible = backupStatus != null) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateDark),
                                border = BorderStroke(1.dp, SlateLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = EmeraldPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = backupStatus ?: "",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Encrypted Peer-to-Peer Synchronizer
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = CyberTeal)
                        }

                        Column {
                            Text(
                                text = "Peer-to-Peer Syncing",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Local NSD handshake & AES encryption",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Discovers other active NetShield instances running on your Wi-Fi network and securely transmits filters utilizing an encrypted handshake.",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.syncWithPeerInstance() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("p2p_sync_button")
                    ) {
                        Icon(Icons.Default.LeakAdd, contentDescription = null, tint = SlateDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Initiate Secure P2P Sync", color = SlateDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    AnimatedVisibility(visible = syncStatus != null) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateDark),
                                border = BorderStroke(1.dp, SlateLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = CyberTeal,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = syncStatus ?: "",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Setup Routing instructions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AlertOrange)
                        Text(
                            text = "Smart TV & Device Setup Guide",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "To route another local device's traffic (such as a Smart TV, PC, or iPad) through NetShield's ad-blocker:",
                        color = TextLightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InstructionStep(step = "1", text = "Open Wi-Fi Settings on the target device.")
                    InstructionStep(step = "2", text = "Edit network configurations and change IP/DNS settings from DHCP to 'Static'.")
                    InstructionStep(step = "3", text = "Enter DNS 1 as: $localIp")
                    InstructionStep(step = "4", text = "Save configuration. All advertisements will immediately be dropped.")
                }
            }
        }
    }
}

@Composable
fun InstructionStep(step: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(SlateLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = AlertOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = text, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}
