package com.websiteblocker.app.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.websiteblocker.app.BuildConfig
import com.websiteblocker.app.vpn.*

@Composable
fun ProtectionCard(
    state: ProtectionState,
    appBlockingEnabled: Boolean,
    appBlockingAvailable: Boolean,
    enable: () -> Unit,
    stop: () -> Unit,
    enableAppBlocking: () -> Unit,
    details: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (
                    state.phase == ProtectionPhase.ON &&
                        (!appBlockingAvailable || appBlockingEnabled)
                )
                    "Protection is on"
                else "Finish protection setup",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (appBlockingAvailable)
                    "Website and app controls are separate Android permissions. Enable both for the strongest blocking."
                else
                    "Active popular-service schedules block all network traffic from their installed apps through the private on-device VPN.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ProtectionStatusRow("Websites", state.message, state.phase == ProtectionPhase.ON)
            if (state.phase == ProtectionPhase.STARTING)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            else
                OutlinedButton(
                    onClick = if (state.phase == ProtectionPhase.ON) stop else enable,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.phase == ProtectionPhase.ON) "Turn off blocking"
                        else "Enable Blocking"
                    )
                }
            if (appBlockingAvailable) {
                ProtectionStatusRow(
                    "Popular apps",
                    if (appBlockingEnabled) "App blocking access is on"
                    else "App blocking access is off",
                    appBlockingEnabled,
                )
                if (!appBlockingEnabled)
                    Button(onClick = enableAppBlocking, modifier = Modifier.fillMaxWidth()) {
                        Text("Enable app blocking")
                    }
            }
            TextButton(onClick = details) { Text("How protection works") }
        }
    }
}

@Composable
private fun ProtectionStatusRow(title: String, status: String, ready: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            if (ready) "On" else "Off",
            color =
                if (ready) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun PermissionExplanation(onDismiss: () -> Unit, onEnable: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Private, on-device protection") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Focus uses an on-device VPN to block selected apps and websites during your chosen schedule. Your browsing activity is not collected or sent to a server."
                )
                if (BuildConfig.ACCESSIBILITY_APP_BLOCKING)
                    Text(
                        "Popular-app blocking uses Android Accessibility only to see the name of the app currently on screen. During an active schedule it returns you to Home. It does not read or store screen content."
                    )
                else
                    Text(
                        "Popular-service schedules use package-scoped VPN routing to stop all IPv4 and IPv6 traffic from their installed apps. While an installed-app schedule is active, app blocking takes priority over custom website filtering."
                    )
                Text(
                    "Only one VPN can be active. Enabling this replaces another VPN unless Android prevents it."
                )
                Text(
                    "Use standard DNS: encrypted DNS, browser Secure DNS and cached connections can bypass blocking. Turn off Private DNS and browser Secure DNS when testing. This is a focus aid, not tamper-proof access control."
                )
            }
        },
        confirmButton = { TextButton(onClick = onEnable) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
