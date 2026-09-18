package com.websiteblocker.app.ui.rule

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.websiteblocker.app.ui.formatTime
import com.websiteblocker.app.ui.ServiceIcon
import com.websiteblocker.app.domain.ServiceCatalog
import com.websiteblocker.app.domain.ServiceProfile
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleScreen(state: RuleState, editing: Boolean, vm: RuleViewModel, back: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24 = DateFormat.is24HourFormat(context)
    LaunchedEffect(state.saved) { if (state.saved) back() }
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
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit block" else "Add block") },
                navigationIcon = { TextButton(onClick = back) { Text("Back") } },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Set a little space aside", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a popular service or add any website.")
            Text("Popular services", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceCatalog.popular.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { profile ->
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
            }
            OutlinedCard(
                onClick = vm::customWebsite,
                enabled = !state.loading && !state.saving,
                colors =
                    CardDefaults.outlinedCardColors(
                        containerColor =
                            if (state.customSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                    ),
                border =
                    BorderStroke(
                        if (state.customSelected) 2.dp else 1.dp,
                        if (state.customSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Custom website", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Enter any site address",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            state.targetError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.customSelected)
                OutlinedTextField(
                    value = state.domain,
                    onValueChange = vm::domain,
                    label = { Text("Website") },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                    isError = state.domainError != null,
                    supportingText = {
                        Text(state.domainError ?: "You can paste a full website address.")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    enabled = !state.loading && !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            Text("Quiet hours", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { pick(state.start, vm::start) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving && !state.loading,
            ) {
                Text("Block from  ${formatTime(state.start, is24)}")
            }
            OutlinedButton(
                onClick = { pick(state.end, vm::end) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving && !state.loading,
            ) {
                Text("Available at  ${formatTime(state.end, is24)}")
            }
            if (state.start > state.end)
                Text(
                    "Ends the following morning. Selected days are when blocking begins.",
                    style = MaterialTheme.typography.bodySmall,
                )
            state.timeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Repeat on", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { vm.days(127) }, enabled = !state.saving && !state.loading) {
                    Text("Every day")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val bit = 1 shl (day.value - 1)
                    FilterChip(
                        modifier =
                            Modifier.semantics {
                                contentDescription = day.getDisplayName(TextStyle.FULL, locale)
                            },
                        selected = state.days and bit != 0,
                        onClick = { vm.days(state.days xor bit) },
                        enabled = !state.saving && !state.loading,
                        label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    )
                }
            }
            state.daysError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Schedule enabled", style = MaterialTheme.typography.titleMedium)
                Switch(
                    modifier = Modifier.semantics { contentDescription = "Schedule enabled" },
                    checked = state.enabled,
                    onCheckedChange = vm::enabled,
                    enabled = !state.saving && !state.loading,
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = vm::save,
                enabled = !state.loading && !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(if (state.saving) "Saving…" else "Save schedule")
            }
            Text(
                "Saving a rule does not turn on protection. Enable blocking from the home screen.",
                style = MaterialTheme.typography.bodySmall,
            )
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
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
        border =
            BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier = modifier.heightIn(min = 112.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ServiceIcon(profile)
            Text(profile.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}
