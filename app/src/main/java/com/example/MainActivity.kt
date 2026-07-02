package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.BlocklistScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.NetShieldViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: NetShieldViewModel = viewModel()
                val isDnsRunning by viewModel.isDnsServerRunning.collectAsState()
                val isVpnRunning by viewModel.isVpnRunning.collectAsState()

                val context = LocalContext.current

                // 1. Configure the standard Android VPN permission launcher
                val vpnPrepareLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.toggleVpn(context)
                    } else {
                        Toast.makeText(context, "VPN Permission declined", Toast.LENGTH_SHORT).show()
                    }
                }

                // Custom VPN toggle handler with permission check
                val onVpnToggleRequested = {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnPrepareLauncher.launch(intent)
                    } else {
                        viewModel.toggleVpn(context)
                    }
                }

                // 2. Request POST_NOTIFICATIONS for Android 13+ status bar reports
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(context, "Notifications disabled. VPN status reports might not display.", Toast.LENGTH_LONG).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // App State: Main tab selection
                var currentTab by remember { mutableStateOf("dashboard") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        NetShieldTopAppBar(
                            isProtected = isDnsRunning || isVpnRunning,
                            isDnsRunning = isDnsRunning,
                            isVpnRunning = isVpnRunning
                        )
                    },
                    bottomBar = {
                        NetShieldBottomNavigationBar(
                            selectedTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    },
                    containerColor = SlateDark
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                            },
                            label = "tab_fade_transition"
                        ) { targetTab ->
                            when (targetTab) {
                                "dashboard" -> DashboardScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "devices" -> DevicesScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "blocklist" -> BlocklistScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "backup" -> BackupScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetShieldTopAppBar(
    isProtected: Boolean,
    isDnsRunning: Boolean,
    isVpnRunning: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "top_bar_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Glow logo badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isProtected) EmeraldPrimary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f))
                        .border(1.dp, if (isProtected) EmeraldPrimary.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield Logo",
                        tint = if (isProtected) EmeraldPrimary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "NETSHIELD",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "V1.0 PRO • OFFLINE-FIRST ENGINE",
                        color = TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        actions = {
            // Live Status Banner Badge
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isProtected) EmeraldPrimary.copy(alpha = 0.12f) else SlateLight
                    )
                    .border(
                        1.dp,
                        if (isProtected) EmeraldPrimary.copy(alpha = 0.3f) else SlateLight,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isProtected) EmeraldPrimary.copy(alpha = pulseAlpha) else Color.Gray
                            )
                    )
                    Text(
                        text = if (isProtected) "PROTECTED" else "UNFILTERED",
                        color = if (isProtected) EmeraldPrimary else TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SlateDark,
            titleContentColor = TextWhite
        ),
        modifier = Modifier.testTag("netshield_top_app_bar")
    )
}

@Composable
fun NetShieldBottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = SlateMedium,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("netshield_bottom_navigation_bar")
    ) {
        NavigationBarItem(
            selected = selectedTab == "dashboard",
            onClick = { onTabSelected("dashboard") },
            icon = {
                Icon(
                    imageVector = if (selectedTab == "dashboard") Icons.Default.Shield else Icons.Default.Shield,
                    contentDescription = "Dashboard"
                )
            },
            label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateDark,
                unselectedIconColor = TextGray,
                selectedTextColor = EmeraldPrimary,
                unselectedTextColor = TextGray,
                indicatorColor = EmeraldPrimary
            )
        )

        NavigationBarItem(
            selected = selectedTab == "devices",
            onClick = { onTabSelected("devices") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = "Devices"
                )
            },
            label = { Text("Devices", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateDark,
                unselectedIconColor = TextGray,
                selectedTextColor = EmeraldPrimary,
                unselectedTextColor = TextGray,
                indicatorColor = EmeraldPrimary
            )
        )

        NavigationBarItem(
            selected = selectedTab == "blocklist",
            onClick = { onTabSelected("blocklist") },
            icon = {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Filters"
                )
            },
            label = { Text("Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateDark,
                unselectedIconColor = TextGray,
                selectedTextColor = EmeraldPrimary,
                unselectedTextColor = TextGray,
                indicatorColor = EmeraldPrimary
            )
        )

        NavigationBarItem(
            selected = selectedTab == "backup",
            onClick = { onTabSelected("backup") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Sync"
                )
            },
            label = { Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SlateDark,
                unselectedIconColor = TextGray,
                selectedTextColor = EmeraldPrimary,
                unselectedTextColor = TextGray,
                indicatorColor = EmeraldPrimary
            )
        )
    }
}
