package com.usefocus.app.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usefocus.app.BuildConfig
import com.usefocus.app.vpn.*

@Composable
fun AppBlockingExplanation(onDismiss: () -> Unit, onEnable: () -> Unit, onAppInfo: () -> Unit, onPrivacy: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable app blocking") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Focus uses Android Accessibility to detect the app on screen and return you to Home when its schedule is active. It does not read screen content, messages or passwords. Observed foreground app names stay in memory and are not recorded or shared.")
                Text("In Android Accessibility settings, open Downloaded apps (or Installed apps), choose App blocking, and turn it on. Website blocking stays active through the VPN.")
                Text("For a sideloaded APK, Android may show Restricted setting. If you trust this build, open Focus app info, tap the three-dot menu and Allow restricted settings, then return to Accessibility. The option and wording depend on your device.")
                TextButton(onClick = onAppInfo) { Text("Open Focus app info") }
                TextButton(onClick = onPrivacy) { Text("Privacy policy") }
            }
        },
        confirmButton = { TextButton(onClick = onEnable) { Text("Agree and open settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

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
                    "Website schedules block matching DNS requests through the on-device VPN.",
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
fun PermissionExplanation(onDismiss: () -> Unit, onEnable: () -> Unit, onPrivacy: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Private, on-device protection") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Focus uses Android's VPN permission to inspect DNS domain names on this device and block names matching your schedules. This runs in the background while protection is on. Blocked queries stay on the device; Focus does not keep a DNS or browsing history.")
                Text("Allowed DNS queries are sent to Cloudflare's 1.1.1.1 public resolver using encrypted HTTPS. Cloudflare receives the queried domain name, DNS request and your IP address to resolve it. The developer does not receive these queries. This is website filtering, not a VPN that hides your IP address or encrypts all browsing traffic.")
                Text("You can decline, or stop protection at any time from Settings or the notification. App blocking asks for separate Accessibility consent. Advertising never receives data from this VPN or the Accessibility service.")
                TextButton(onClick = onPrivacy) { Text("Privacy policy and providers") }
                Text(
                    "Only one VPN can be active. Enabling this replaces another VPN unless Android prevents it."
                )
                Text(
                    "Use standard DNS: encrypted DNS, browser Secure DNS and cached connections can bypass blocking. Turn off Private DNS and browser Secure DNS when testing. This is a focus aid, not tamper-proof access control."
                )
            }
        },
        confirmButton = { TextButton(onClick = onEnable) { Text("Agree and continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
