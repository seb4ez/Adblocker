package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.BlockedDomain
import com.example.ui.theme.*
import com.example.ui.viewmodel.NetShieldViewModel

@Composable
fun BlocklistScreen(
    viewModel: NetShieldViewModel,
    modifier: Modifier = Modifier
) {
    val blockedDomains by viewModel.blockedDomains.collectAsState()

    var domainInput by remember { mutableStateOf("") }
    var isWhitelistInput by remember { mutableStateOf(false) } // false = Blacklist, true = Whitelist
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Form: Add custom rule
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateMedium),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Custom Filter Rule",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Field
                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = {
                            domainInput = it
                            errorMessage = null
                        },
                        label = { Text("Domain Name (e.g., ads.example.com)", fontSize = 12.sp, color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SlateLight,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextLightGray
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("domain_input_field"),
                        isError = errorMessage != null
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = AlertRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Rule Selector (Blacklist vs Whitelist)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Rule Type Action",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isWhitelistInput) "Whitelist (Always bypass blocklists)" else "Blacklist (Block and drop connection)",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }

                        // Toggle Buttons (Custom Segmented Control)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateLight)
                                .padding(2.dp)
                        ) {
                            Button(
                                onClick = { isWhitelistInput = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isWhitelistInput) AlertRed.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (!isWhitelistInput) AlertRed else TextGray
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Block", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { isWhitelistInput = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isWhitelistInput) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (isWhitelistInput) EmeraldPrimary else TextGray
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Allow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val trimmed = domainInput.trim()
                            if (trimmed.isEmpty()) {
                                errorMessage = "Domain name cannot be empty."
                            } else if (!trimmed.contains(".") || trimmed.length < 4) {
                                errorMessage = "Please enter a valid domain name structure."
                            } else {
                                viewModel.addCustomDomain(trimmed, isWhitelistInput)
                                domainInput = ""
                                errorMessage = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("submit_rule_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SlateDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Rule", color = SlateDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                text = "CUSTOM ACTIVE RULES (${blockedDomains.size})",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Rules List
        if (blockedDomains.isEmpty()) {
            item {
                EmptyRulesPlaceholder()
            }
        } else {
            items(blockedDomains) { rule ->
                RuleItemRow(
                    rule = rule,
                    onDelete = { viewModel.removeCustomDomain(rule.domain) }
                )
            }
        }
    }
}

@Composable
fun RuleItemRow(
    rule: BlockedDomain,
    onDelete: () -> Unit
) {
    val isWhitelist = rule.isWhitelist

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rule_row_${rule.domain}"),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isWhitelist) EmeraldPrimary.copy(alpha = 0.1f) else AlertRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isWhitelist) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isWhitelist) EmeraldPrimary else AlertRed,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.domain,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isWhitelist) "Whitelist Rule • Connection Allowed" else "Blacklist Rule • Connection Blocked",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_rule_${rule.domain}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Delete Rule",
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyRulesPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No custom rules configured",
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "NetShield is operating with standard system ad-blockers. Add custom domains above to specify direct filters.",
            color = TextGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
