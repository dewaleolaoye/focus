package com.usefocus.app.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usefocus.app.R
import com.usefocus.app.domain.ServiceCatalog
import com.usefocus.app.ui.InstalledAppIcon
import com.usefocus.app.ui.ServiceIcon
import com.usefocus.app.ui.WebsiteIcon
import com.usefocus.app.ui.formatDuration
import com.usefocus.app.ui.formatTime
import com.usefocus.app.ui.formatUpcoming
import com.usefocus.app.ui.ads.AnchoredAdaptiveBanner
import com.usefocus.app.vpn.ProtectionPhase

@Composable
fun HomeScreen(
    state: HomeState,
    add: () -> Unit,
    edit: (Long) -> Unit,
    enable: () -> Unit,
    settings: () -> Unit,
    dismissError: () -> Unit,
    privacy: () -> Unit,
    appBlockingEnabled: Boolean,
    enableAppBlocking: () -> Unit,
) {
    val firstRun =
        !state.loading &&
            state.rows.isEmpty() &&
            state.protection.phase != ProtectionPhase.ON
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (firstRun) {
            FirstRunHome(
                phase = state.protection.phase,
                message = state.protection.message,
                enable = enable,
                privacy = privacy,
                contentPadding = padding,
            )
        } else {
            Dashboard(
                state = state,
                add = add,
                edit = edit,
                enable = enable,
                settings = settings,
                contentPadding = padding,
                appBlockingEnabled = appBlockingEnabled,
                enableAppBlocking = enableAppBlocking,
            )
        }
    }
    state.error?.let {
        AlertDialog(
            onDismissRequest = dismissError,
            title = { Text("Could not update schedules") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = dismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun FirstRunHome(
    privacy: () -> Unit,
    phase: ProtectionPhase,
    message: String,
    enable: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding).navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BrandHeader() }
        item { TextButton(onClick = privacy) { Text("Privacy policy") } }
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_focus_leaves),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(64.dp),
                    )
                    Text(
                        when (phase) {
                            ProtectionPhase.STARTING -> "Starting protection"
                            ProtectionPhase.NEEDS_REACTIVATION -> "Protection needs attention"
                            else -> "Make space for what matters"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        when (phase) {
                            ProtectionPhase.STARTING -> "Checking private, on-device enforcement…"
                            ProtectionPhase.NEEDS_REACTIVATION -> message
                            else -> "Enable private, on-device blocking before creating your first schedule."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = enable,
                        enabled = phase != ProtectionPhase.STARTING,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            if (phase == ProtectionPhase.NEEDS_REACTIVATION) "Fix protection"
                            else if (phase == ProtectionPhase.STARTING) "Starting…"
                            else "Enable Blocking"
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Your schedules and blocking activity stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Dashboard(
    state: HomeState,
    add: () -> Unit,
    edit: (Long) -> Unit,
    enable: () -> Unit,
    settings: () -> Unit,
    contentPadding: PaddingValues,
    appBlockingEnabled: Boolean,
    enableAppBlocking: () -> Unit,
) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Extra bottom padding (80.dp) ensures list items and "Add schedule" button
            // are never covered or obscured by the anchored adaptive banner
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { BrandHeader(settings) }
            if (state.loading) {
                item { LoadingCard() }
            } else {
                item {
                    ProtectionSummaryCard(
                        state = state,
                        appBlockingEnabled = appBlockingEnabled,
                        enable = enable,
                        settings = settings,
                    )
                }
                if (!appBlockingEnabled && state.rows.any { (it.rule.serviceId != null || it.rule.packageName != null) && it.rule.enabled }) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("App blocking needs setup", style = MaterialTheme.typography.titleMedium)
                                Text("Website protection uses the VPN. To block apps, enable App blocking in Android Accessibility settings.")
                                Button(onClick = enableAppBlocking) { Text("Enable app blocking") }
                            }
                        }
                    }
                }
                if (state.rows.isEmpty()) {
                    item { EmptySchedules(add) }
                } else {
                    item {
                        BlockingSummaryCard(
                            state = state,
                            is24 = is24,
                            onClick = {
                                val row =
                                    when (state.dashboardStatus) {
                                        DashboardStatus.ACTIVE ->
                                            state.rows.firstOrNull { it.status == RuleUiStatus.ACTIVE }
                                        DashboardStatus.IDLE ->
                                            state.rows.filter { it.nextStart != null }
                                                .minByOrNull { it.nextStart!! }
                                        else -> null
                                    }
                                if (row != null) edit(row.rule.id) else settings()
                            },
                        )
                    }
                    item {
                        Text(
                            "Schedules",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(state.rows, key = { it.rule.id }) { row ->
                        TargetScheduleRow(
                            row = row,
                            now = state.now,
                            is24 = is24,
                            onClick = { edit(row.rule.id) },
                        )
                    }
                    item { AddScheduleButton(add) }
                }
            }
        }

        // Anchored adaptive banner at bottom of dashboard, above system navigation bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            AnchoredAdaptiveBanner()
        }
    }
}

@Composable
private fun ProtectionSummaryCard(
    state: HomeState,
    appBlockingEnabled: Boolean,
    enable: () -> Unit,
    settings: () -> Unit,
) {
    val title: String
    val status: String
    val detail: String
    val icon =
        when (state.dashboardStatus) {
            DashboardStatus.OFF -> Icons.Rounded.PauseCircleOutline
            DashboardStatus.NEEDS_ATTENTION -> Icons.Rounded.ErrorOutline
            else -> Icons.Rounded.Shield
        }
    if (state.protection.phase == ProtectionPhase.ON && !appBlockingEnabled &&
        state.rows.any { it.rule.serviceId != null && it.rule.enabled }) {
        title = "Website protection is on"
        status = "App blocking needs setup"
        detail = "Enable App blocking access to protect the installed apps too."
    } else when (state.dashboardStatus) {
        DashboardStatus.NO_SCHEDULES -> {
            title = "Protection is on"
            status = "Ready for your first schedule"
            detail = "Add an app or website to begin."
        }
        DashboardStatus.ACTIVE -> {
            title = "Protection is on"
            status = "Blocking active"
            detail =
                "${state.activeTargetCount} ${if (state.activeTargetCount == 1) "target" else "targets"} blocked · " +
                    "${state.configuredTargetCount} configured"
        }
        DashboardStatus.IDLE -> {
            title = "Protection is on"
            status = "No blocking active right now"
            detail = "${state.configuredTargetCount} ${if (state.configuredTargetCount == 1) "target" else "targets"} configured"
        }
        DashboardStatus.OFF -> {
            title = "Protection is off"
            status = "Schedules are paused"
            detail = "Your schedules are saved, but blocking is currently disabled."
        }
        DashboardStatus.NEEDS_ATTENTION -> {
            title = "Protection needs attention"
            status = "Blocking is not operational"
            detail = state.protection.message
        }
        DashboardStatus.STARTING -> {
            title = "Starting protection"
            status = "Checking VPN enforcement"
            detail = state.protection.message
        }
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color =
            if (state.dashboardStatus == DashboardStatus.NEEDS_ATTENTION)
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .65f),
                ) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.titleSmall)
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = settings) {
                    Icon(Icons.Rounded.Info, contentDescription = "How blocking works")
                }
            }
            if (
                state.dashboardStatus == DashboardStatus.OFF ||
                    state.dashboardStatus == DashboardStatus.NEEDS_ATTENTION
            ) {
                Button(onClick = enable, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (state.dashboardStatus == DashboardStatus.OFF) "Enable Blocking"
                        else "Fix protection"
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockingSummaryCard(
    state: HomeState,
    is24: Boolean,
    onClick: () -> Unit,
) {
    val title: String
    val subtitle: String
    when (state.dashboardStatus) {
        DashboardStatus.ACTIVE -> {
            title =
                state.currentBlockingEnd?.let { "Quiet until ${formatTime(it.hour * 60 + it.minute, is24)}" }
                    ?: "Blocking active"
            subtitle =
                state.currentBlockingEnd?.let {
                    "Blocking ends in ${formatDuration(state.now, it)}"
                } ?: "Blocking is active now"
        }
        DashboardStatus.IDLE -> {
            title =
                state.nextBlockingStart?.let {
                    "Next block ${formatUpcoming(it, state.now, is24)}"
                } ?: "No upcoming blocks"
            subtitle =
                if (state.nextTargetNames.isEmpty()) "Enable a schedule to start automatically."
                else state.nextTargetNames.joinToString(" and ")
        }
        DashboardStatus.OFF -> {
            title = "Schedules paused"
            subtitle = "Enable protection to resume automatic blocking."
        }
        DashboardStatus.NEEDS_ATTENTION -> {
            title = "Blocking unavailable"
            subtitle = "Resolve protection setup to enforce your schedules."
        }
        DashboardStatus.STARTING -> {
            title = "Starting blocking"
            subtitle = "This should only take a moment."
        }
        DashboardStatus.NO_SCHEDULES -> return
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .55f)) {
                Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun TargetScheduleRow(
    row: RuleRow,
    now: java.time.ZonedDateTime,
    is24: Boolean,
    onClick: () -> Unit,
) {
    val name = ServiceCatalog.displayName(row.rule)
    val detail =
        when (row.status) {
            RuleUiStatus.ACTIVE ->
                row.activeUntil?.let {
                    "Blocked until ${formatTime(it.hour * 60 + it.minute, is24)}"
                } ?: "Blocking now"
            RuleUiStatus.SCHEDULED ->
                row.nextStart?.let { "Starts ${formatUpcoming(it, now, is24)}" }
                    ?: "No upcoming time"
            RuleUiStatus.DISABLED -> "Schedule disabled"
            RuleUiStatus.PROTECTION_OFF -> "Global protection is off"
            RuleUiStatus.NEEDS_ATTENTION -> "Protection needs attention"
            RuleUiStatus.STARTING -> "Protection is starting"
        }
    val (label, container, foreground) = statusStyle(row.status)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (row.rule.packageName != null) {
                InstalledAppIcon(row.rule.packageName, size = 48.dp)
            } else {
                ServiceCatalog.find(row.rule.serviceId)?.let { ServiceIcon(it, 48.dp) }
                    ?: WebsiteIcon(row.rule.domain, 48.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = container) {
                Text(
                    label,
                    color = foreground,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun statusStyle(status: RuleUiStatus): Triple<String, Color, Color> =
    when (status) {
        RuleUiStatus.ACTIVE ->
            Triple("Active", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        RuleUiStatus.SCHEDULED ->
            Triple("Scheduled", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        RuleUiStatus.DISABLED ->
            Triple("Off", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant)
        RuleUiStatus.PROTECTION_OFF ->
            Triple("Protection off", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant)
        RuleUiStatus.NEEDS_ATTENTION ->
            Triple("Attention", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        RuleUiStatus.STARTING ->
            Triple("Starting", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }

@Composable
private fun EmptySchedules(add: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("No schedules yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Choose an app or website and decide when to block it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AddScheduleButton(add)
        }
    }
}

@Composable
private fun AddScheduleButton(add: () -> Unit) {
    Button(
        onClick = add,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(18.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Add schedule")
    }
}

@Composable
private fun LoadingCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Loading schedules…", modifier = Modifier.padding(22.dp))
    }
}

@Composable
private fun BrandHeader(settings: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_focus_leaves),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )
            Text("Focus", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        settings?.let {
            IconButton(onClick = it) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
            }
        }
    }
}
