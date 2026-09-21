package com.websiteblocker.app.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AgeGroupDialog(select: (AgeGroup) -> Unit, privacy: () -> Unit) {
    AlertDialog(
        onDismissRequest = { select(AgeGroup.UNSPECIFIED) },
        title = { Text("Which age group are you in?") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Focus is intended for adults and teenagers aged 13 and up. Your choice stays on this device. It is used only to decide whether ads may be requested; blocking works without ads.")
                AgeGroup.entries.forEach { group ->
                    OutlinedButton(onClick = { select(group) }, modifier = Modifier.fillMaxWidth()) {
                        Text(group.label)
                    }
                }
                Text("No ads are requested for anyone under 18 or whose age group is unspecified.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = privacy) { Text("Privacy policy") } },
    )
}
