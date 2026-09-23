package com.usefocus.app.ui.rule

import android.app.TimePickerDialog
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usefocus.app.domain.ServiceCatalog
import com.usefocus.app.domain.ServiceProfile
import com.usefocus.app.ui.InstalledAppIcon
import com.usefocus.app.ui.ServiceIcon
import com.usefocus.app.ui.formatDays
import com.usefocus.app.ui.formatTime
import com.usefocus.app.vpn.ProtectionPhase
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleScreen(
    state: RuleState,
    editing: Boolean,
    vm: RuleViewModel,
    enableProtection: () -> Unit,
    back: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val locale = LocalConfiguration.current.locales[0]
    val is24 = DateFormat.is24HourFormat(context)
    var confirmDelete by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.completion) {
        when (state.completion) {
            RuleCompletion.SAVED -> {
                Toast.makeText(
                        context,
                        if (editing) "Changes saved" else "Schedule created",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                back()
            }
            RuleCompletion.DELETED -> {
                Toast.makeText(context, "Schedule deleted", Toast.LENGTH_SHORT).show()
                back()
            }
            null -> Unit
        }
    }

    fun pick(value: Int, changed: (Int) -> Unit) {
        TimePickerDialog(
                context,
                { _, hour, minute -> changed(hour * 60 + minute) },
                value / 60,
                value % 60,
                is24,
            )
            .show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editing) "Edit schedule" else "New schedule",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.loading) item { Text("Loading schedule…") }
            item {
                SectionCard(
                    title = "What do you want to block?",
                    subtitle = "Choose an app or website.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = state.targetTab == TargetTab.APP,
                                onClick = { vm.selectTab(TargetTab.APP) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    Icon(
                                        Icons.Rounded.PhoneAndroid,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                label = { Text("App") },
                                enabled = !state.loading && !state.saving,
                            )
                            SegmentedButton(
                                selected = state.targetTab == TargetTab.WEBSITE,
                                onClick = { vm.selectTab(TargetTab.WEBSITE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    Icon(
                                        Icons.Rounded.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                label = { Text("Website") },
                                enabled = !state.loading && !state.saving,
                            )
                        }

                        if (state.targetTab == TargetTab.APP) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Popular apps",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ServiceCatalog.popular.forEach { profile ->
                                        ServiceCard(
                                            profile = profile,
                                            selected = state.serviceId == profile.id,
                                            enabled = !state.loading && !state.saving,
                                            onClick = { vm.service(profile.id) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }

                            if (state.packageName != null) {
                                SelectedAppHeader(
                                    packageName = state.packageName,
                                    appName = state.appDisplayName ?: state.packageName,
                                    change = { showAppPicker = true },
                                )
                            } else {
                                OutlinedCard(
                                    onClick = { showAppPicker = true },
                                    enabled = !state.loading && !state.saving,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Rounded.Apps,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Other installed apps",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "Select from your device",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.domain,
                                    onValueChange = vm::domain,
                                    label = { Text("Website domain") },
                                    placeholder = { Text("goal.com") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Language, contentDescription = null)
                                    },
                                    singleLine = true,
                                    isError = state.domainError != null,
                                    supportingText = {
                                        Text(
                                            state.domainError
                                                ?: "Example: goal.com or https://goal.com · subdomains included"
                                        )
                                    },
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType = KeyboardType.Uri,
                                            imeAction = ImeAction.Next,
                                        ),
                                    keyboardActions =
                                        KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                    enabled = !state.loading && !state.saving,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    "Blocking works at the domain level. Paths such as /news cannot be blocked separately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        state.targetError?.let { InlineError(it) }
                    }
                }
            }
            item {
                BlockingHoursCard(
                    start = formatTime(state.start, is24),
                    end = formatTime(state.end, is24),
                    overnight = state.start > state.end,
                    enabled = !state.loading && !state.saving,
                    onStartClick = { pick(state.start, vm::start) },
                    onEndClick = { pick(state.end, vm::end) },
                ) {
                    state.timeError?.let { InlineError(it) }
                }
            }
            item {
                SectionCard(title = "Repeat", subtitle = formatDays(state.days).ifEmpty { "Choose at least one day." }) {
                    if (state.days != 127) {
                        TextButton(
                            onClick = { vm.days(127) },
                            enabled = !state.saving && !state.loading,
                        ) { Text("Every day") }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val bit = 1 shl (day.value - 1)
                            FilterChip(
                                modifier =
                                    Modifier.semantics {
                                        contentDescription =
                                            day.getDisplayName(TextStyle.FULL, locale)
                                    },
                                selected = state.days and bit != 0,
                                onClick = { vm.days(state.days xor bit) },
                                enabled = !state.saving && !state.loading,
                                label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                                leadingIcon =
                                    if (state.days and bit != 0) {
                                        {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else null,
                            )
                        }
                    }
                    state.daysError?.let { InlineError(it) }
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable this schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Run automatically on selected days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            modifier = Modifier.semantics { contentDescription = "Enable this schedule" },
                            checked = state.enabled,
                            onCheckedChange = vm::enabled,
                            enabled = !state.saving && !state.loading,
                        )
                    }
                }
            }
            if (state.protectionPhase != ProtectionPhase.ON) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                if (state.protectionPhase == ProtectionPhase.NEEDS_REACTIVATION)
                                    "Protection needs attention"
                                else "Global protection is currently off",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "This schedule will start working when protection is enabled.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = enableProtection) { Text("Enable protection") }
                        }
                    }
                }
            }
            state.error?.let { item { InlineError(it) } }
            item {
                Button(
                    onClick = vm::save,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                ) {
                    Text(
                        when {
                            state.saving -> "Saving…"
                            editing -> "Save changes"
                            else -> "Save schedule"
                        }
                    )
                }
            }
            if (editing) {
                item {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                        Text("Delete schedule", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this schedule?") },
            text = { Text("This cannot be undone. Other schedules for the same target will remain.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.delete()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    if (showAppPicker) {
        AppPickerBottomSheet(
            onDismiss = { showAppPicker = false },
            onAppSelected = { pkg, label ->
                vm.installedApp(pkg, label)
            },
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun ServiceCard(
    profile: ServiceProfile,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
            ),
        border =
            BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier =
            modifier
                .height(56.dp)
                .semantics { contentDescription = "${profile.name}${if (selected) ", selected" else ""}" },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ServiceIcon(profile, 36.dp)
        }
    }
}

@Composable
private fun SelectedAppHeader(
    packageName: String,
    appName: String,
    change: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InstalledAppIcon(packageName = packageName, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = change) { Text("Change") }
        }
    }
}

@Composable
private fun BlockingHoursCard(
    start: String,
    end: String,
    overnight: Boolean,
    enabled: Boolean,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    error: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Blocking hours",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Choose when blocking starts and stops.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TimeField(
                    label = "Start blocking",
                    time = start,
                    enabled = enabled,
                    onClick = onStartClick,
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = "Stop blocking",
                    time = end,
                    enabled = enabled,
                    onClick = onEndClick,
                    modifier = Modifier.weight(1f),
                )
            }
            if (overnight) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        "Stops the following morning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            error()
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    time: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 88.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(time, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InlineError(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
