package com.websiteblocker.app.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.websiteblocker.app.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(back: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val text = remember {
        context.assets.open("privacy-policy.txt").bufferedReader().use { it.readText() }
            .replace("{{DEVELOPER_NAME}}", BuildConfig.DEVELOPER_NAME.ifBlank { "the Focus developer (publisher details pending for this development build)" })
            .replace("{{CONTACT_EMAIL}}", BuildConfig.PRIVACY_CONTACT_EMAIL.ifBlank { "the developer through your app download source" })
            .replace("{{POLICY_URL}}", BuildConfig.PRIVACY_POLICY_URL.ifBlank { "the publisher's website when this app is released" })
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Privacy policy") }, navigationIcon = {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SelectionContainer { Text(text, style = MaterialTheme.typography.bodyMedium) }
            val links = listOf(
                "Cloudflare DNS privacy" to "https://developers.cloudflare.com/1.1.1.1/privacy/public-dns-resolver/",
                "Google privacy policy" to "https://policies.google.com/privacy",
                "Google advertising data" to "https://developers.google.com/admob/android/privacy/play-data-disclosure",
            ) + if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) listOf("Published Focus privacy policy" to BuildConfig.PRIVACY_POLICY_URL) else emptyList()
            links.forEach { (label, url) ->
                TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) { Text(label) }
            }
        }
    }
}
