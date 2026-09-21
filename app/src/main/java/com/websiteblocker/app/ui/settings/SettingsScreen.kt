package com.websiteblocker.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.websiteblocker.app.vpn.ProtectionPhase
import com.websiteblocker.app.vpn.ProtectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    protection: ProtectionState,
    appBlockingAvailable: Boolean,
    appBlockingEnabled: Boolean,
    back: () -> Unit,
    enable: () -> Unit,
    stop: () -> Unit,
    enableAppBlocking: () -> Unit,
    privacy: () -> Unit,
    adsPrivacy: com.websiteblocker.app.privacy.AdsPrivacyState,
    ageGroup: com.websiteblocker.app.privacy.AgeGroup?,
    changeAgeGroup: () -> Unit,
    manageAdsPrivacy: () -> Unit,
    revokeAppBlocking: () -> Unit,
    revokeVpn: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(0.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .55f),
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp).size(32.dp),
                            )
                        }
                        Text(
                            when (protection.phase) {
                                ProtectionPhase.ON -> if (appBlockingEnabled) "Protection is on" else "Website protection is on"
                                ProtectionPhase.OFF -> "Protection is off"
                                ProtectionPhase.STARTING -> "Starting protection"
                                ProtectionPhase.NEEDS_REACTIVATION -> "Protection needs attention"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(protection.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (protection.phase == ProtectionPhase.ON) {
                            OutlinedButton(
                                onClick = stop,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) { Text("Turn off blocking") }
                        } else if (protection.phase != ProtectionPhase.STARTING) {
                            Button(
                                onClick = enable,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) {
                                Text(
                                    if (protection.phase == ProtectionPhase.OFF) "Enable Blocking"
                                    else "Fix protection"
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = privacy, modifier = Modifier.fillMaxWidth()) { Text("Privacy policy") }
                    OutlinedButton(onClick = changeAgeGroup, modifier = Modifier.fillMaxWidth()) {
                        Text("Age group: ${ageGroup?.label ?: "Not specified"}")
                    }
                    OutlinedButton(onClick = manageAdsPrivacy, enabled = !adsPrivacy.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (adsPrivacy.busy) "Checking privacy choices…" else "Advertising privacy choices")
                    }
                    adsPrivacy.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (!adsPrivacy.privacyOptionsRequired && !adsPrivacy.busy && adsPrivacy.message == null)
                        Text("Google has not requested a privacy form for this device. You can check again here.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = revokeVpn, modifier = Modifier.fillMaxWidth()) {
                        Text("Withdraw website protection consent")
                    }
                }
            }
            item {
                InfoCard(
                    icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    title = "How blocking works",
                    body =
                        "Popular app schedules return you to Home when a selected app opens. Enable App blocking in Android Accessibility settings. The on-device VPN blocks matching website domains at the same time.",
                )
            }
            item {
                InfoCard(
                    icon = { Icon(Icons.Rounded.PrivacyTip, contentDescription = null) },
                    title = "Private by design",
                    body =
                        "Rules stay on your device. Allowed DNS requests go to Cloudflare over HTTPS; blocked requests stay local. Google AdMob serves ads after privacy checks. Read the privacy policy for data use, retention and your choices.",
                )
            }
            item {
                InfoCard(
                    icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    title = "A note about VPNs",
                    body =
                        "Android allows one VPN at a time. Private DNS, browser Secure DNS and cached connections may affect website blocking.",
                )
            }
            if (appBlockingAvailable) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        elevation = CardDefaults.elevatedCardElevation(0.dp),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("On-screen app blocking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (appBlockingEnabled) "Accessibility app blocking is enabled."
                                else "App blocking is not connected. Enable App blocking in Android Accessibility settings to prevent selected apps from staying open during quiet hours.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (appBlockingEnabled) {
                                OutlinedButton(onClick = revokeAppBlocking, modifier = Modifier.fillMaxWidth()) {
                                    Text("Withdraw app blocking consent")
                                }
                            }
                            if (!appBlockingEnabled) {
                                Button(onClick = enableAppBlocking, modifier = Modifier.fillMaxWidth()) {
                                    Text("Enable app blocking")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) { icon() }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
