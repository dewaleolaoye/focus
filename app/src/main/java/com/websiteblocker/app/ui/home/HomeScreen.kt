package com.websiteblocker.app.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.websiteblocker.app.R
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.domain.ServiceCatalog
import com.websiteblocker.app.ui.ServiceIcon
import com.websiteblocker.app.ui.formatTime
import com.websiteblocker.app.vpn.ProtectionPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    add: () -> Unit,
    edit: (Long) -> Unit,
    enable: () -> Unit,
    settings: () -> Unit,
    toggle: (BlockRule, Boolean) -> Unit,
    delete: (BlockRule) -> Unit,
    dismissError: () -> Unit,
) {
    var deleting by remember { mutableStateOf<BlockRule?>(null) }
    val enabled = state.protection.phase == ProtectionPhase.ON

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (enabled) {
            EnabledHome(
                state = state,
                add = add,
                edit = edit,
                settings = settings,
                toggle = toggle,
                requestDelete = { deleting = it },
                contentPadding = padding,
            )
        } else {
            FirstRunHome(enable = enable, contentPadding = padding)
        }
    }

    deleting?.let { rule ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${ServiceCatalog.displayName(rule)}?") },
            text = { Text("This removes its saved schedule. Your other blocks will stay in place.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        delete(rule)
                        deleting = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
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
private fun FirstRunHome(enable: () -> Unit, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BrandHeader() }
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(0.dp),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .72f),
                        modifier = Modifier.size(84.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_focus_leaves),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.padding(13.dp),
                        )
                    }
                    Text(
                        "Make space for what matters",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Quiet social apps and distracting websites during the hours you choose.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = enable,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text("Enable Blocking")
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BenefitTile(
                    icon = { Icon(Icons.Rounded.Lock, null) },
                    title = "Popular apps",
                    body = "Instagram, WhatsApp, Facebook and X",
                    modifier = Modifier.weight(1f),
                )
                BenefitTile(
                    icon = { Icon(Icons.Rounded.Language, null) },
                    title = "Any website",
                    body = "Add the sites that take your attention",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(
                "Private by design · Your schedules stay on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun BenefitTile(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 154.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp),
            ) { Box(contentAlignment = Alignment.Center) { icon() } }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EnabledHome(
    state: HomeState,
    add: () -> Unit,
    edit: (Long) -> Unit,
    settings: () -> Unit,
    toggle: (BlockRule, Boolean) -> Unit,
    requestDelete: (BlockRule) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val activeRows = state.rows.filter { it.rule.enabled }
    val serviceOrder = ServiceCatalog.popular.mapIndexed { index, profile -> profile.id to index }.toMap()
    val serviceRows =
        state.rows
            .filter { it.rule.serviceId != null }
            .sortedBy { serviceOrder[it.rule.serviceId] ?: Int.MAX_VALUE }
    val websiteRows = state.rows.filter { it.rule.serviceId == null }
    val current = activeRows.firstOrNull { it.status.startsWith("Active", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BrandHeader(settings) }
        item {
            ProtectionHero(
                protectedCount = serviceRows.count { it.rule.enabled },
                settings = settings,
            )
        }
        item {
            ScheduleBento(
                title = current?.let {
                    "Quiet until ${formatTime(it.rule.endMinute, is24)}"
                } ?: "No block active now",
                subtitle = current?.let { "Blocking is active now" }
                    ?: "Your next schedule will start automatically",
                onClick = current?.let { { edit(it.rule.id) } } ?: settings,
            )
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (!state.loading && state.rows.isEmpty()) {
            item {
                EmptyScheduleCard()
            }
        } else {
            items(
                items = serviceRows.chunked(2),
                key = { pair -> pair.joinToString("-") { it.rule.id.toString() } },
            ) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { row ->
                        ScheduleTile(
                            row = row,
                            edit = { edit(row.rule.id) },
                            delete = { requestDelete(row.rule) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            items(websiteRows, key = { it.rule.id }) { row ->
                WebsiteScheduleTile(
                    row = row,
                    edit = { edit(row.rule.id) },
                    toggle = { toggle(row.rule, it) },
                    delete = { requestDelete(row.rule) },
                )
            }
        }
        item {
            Button(
                onClick = add,
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text("Add schedule", style = MaterialTheme.typography.titleMedium)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun BrandHeader(settings: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_focus_leaves),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(54.dp),
            )
            Text(
                "Focus",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        settings?.let {
            IconButton(onClick = it) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun ProtectionHero(protectedCount: Int, settings: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f),
                            MaterialTheme.colorScheme.surface,
                        )
                    )
                )
                .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                Box(modifier = Modifier.size(124.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 13.dp,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_focus_leaves),
                        contentDescription = null,
                        modifier = Modifier.size(62.dp),
                    )
                }
                Spacer(Modifier.size(18.dp))
                Column(
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "$protectedCount ${if (protectedCount == 1) "app" else "apps"}\nprotected",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Protection is on",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .55f),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text("Apps & websites", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
        }
        FilledIconButton(
            onClick = settings,
            modifier = Modifier.align(Alignment.TopEnd).size(42.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .55f)
            ),
        ) { Icon(Icons.Rounded.Info, contentDescription = "How blocking works") }
    }
}

@Composable
private fun ScheduleBento(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = .55f),
                modifier = Modifier.size(68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun ScheduleTile(
    row: RuleRow,
    edit: () -> Unit,
    delete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = ServiceCatalog.find(row.rule.serviceId)
    Surface(
        modifier = modifier.height(156.dp).clip(RoundedCornerShape(26.dp)).clickable(onClick = edit),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                profile?.let { ServiceIcon(it, 66.dp) }
                IconButton(
                    onClick = delete,
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Delete ${ServiceCatalog.displayName(row.rule)}")
                }
            }
            Text(
                ServiceCatalog.displayName(row.rule),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WebsiteScheduleTile(
    row: RuleRow,
    edit: () -> Unit,
    toggle: (Boolean) -> Unit,
    delete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = edit),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Language, contentDescription = null)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    ServiceCatalog.displayName(row.rule),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(row.status, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = row.rule.enabled, onCheckedChange = toggle)
            IconButton(onClick = delete) {
                Icon(Icons.Rounded.Close, contentDescription = "Delete ${ServiceCatalog.displayName(row.rule)}")
            }
        }
    }
}

@Composable
private fun EmptyScheduleCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Ready for your first schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Choose a popular app or add a website, then set your quiet hours.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
