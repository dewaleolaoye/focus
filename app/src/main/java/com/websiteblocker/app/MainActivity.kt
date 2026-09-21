package com.websiteblocker.app

import android.Manifest
import android.net.VpnService
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.websiteblocker.app.ui.home.*
import com.websiteblocker.app.ui.permission.PermissionExplanation
import com.websiteblocker.app.ui.rule.*
import com.websiteblocker.app.ui.settings.SettingsScreen
import com.websiteblocker.app.ui.theme.WebsiteBlockerTheme
import com.websiteblocker.app.accessibility.AppBlockingAccess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BlockerApplication
        setContent {
            WebsiteBlockerTheme {
                val nav = rememberNavController()
                val lifecycleOwner = LocalLifecycleOwner.current
                var appBlockingPermission by remember {
                    mutableStateOf(
                        BuildConfig.ACCESSIBILITY_APP_BLOCKING &&
                            AppBlockingAccess.isEnabled(this)
                    )
                }
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            appBlockingPermission =
                                BuildConfig.ACCESSIBILITY_APP_BLOCKING &&
                                    AppBlockingAccess.isEnabled(this@MainActivity)
                            app.protection.recoverIfPossible()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val appBlockingConnected by AppBlockingAccess.connection.collectAsStateWithLifecycle()
                val disclosure by app.disclosures.state.collectAsStateWithLifecycle()
                val adsPrivacy by app.adsConsent.state.collectAsStateWithLifecycle()
                val ageGroup by app.audience.state.collectAsStateWithLifecycle()
                var choosingAge by rememberSaveable { mutableStateOf(app.audience.state.value == null) }
                LaunchedEffect(ageGroup) { app.adsConsent.request(this@MainActivity) }
                val appBlockingEnabled = appBlockingPermission && appBlockingConnected && disclosure.accessibilityAccepted
                var appBlockingExplanation by rememberSaveable { mutableStateOf(false) }
                val home: HomeViewModel =
                    viewModel(
                        factory =
                            viewModelFactory {
                                initializer { HomeViewModel(app.repository, app.protection) }
                            }
                    )
                var explanation by rememberSaveable { mutableStateOf(false) }
                val notification =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        /* Notification denial does not prevent user-initiated VPN use. */
                    }
                fun start() {
                    app.protection.start()
                    if (Build.VERSION.SDK_INT >= 33)
                        notification.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                val consent =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) start()
                        else app.protection.consentDenied()
                    }
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        val state by home.state.collectAsStateWithLifecycle()
                        HomeScreen(
                            state,
                            add = { nav.navigate("rule/0") },
                            edit = { nav.navigate("rule/$it") },
                            enable = { explanation = true },
                            settings = { nav.navigate("settings") },
                            dismissError = home::dismissError,
                            appBlockingEnabled = appBlockingEnabled,
                            enableAppBlocking = { appBlockingExplanation = true },
                            privacy = { nav.navigate("privacy") },
                        )
                    }
                    composable("settings") {
                        val state by home.state.collectAsStateWithLifecycle()
                        SettingsScreen(
                            protection = state.protection,
                            appBlockingAvailable = BuildConfig.ACCESSIBILITY_APP_BLOCKING,
                            appBlockingEnabled = appBlockingEnabled,
                            back = { nav.popBackStack() },
                            enable = { explanation = true },
                            stop = {
                                app.protection.stop()
                                nav.popBackStack()
                            },
                            enableAppBlocking = { appBlockingExplanation = true },
                            privacy = { nav.navigate("privacy") },
                            adsPrivacy = adsPrivacy,
                            ageGroup = ageGroup,
                            changeAgeGroup = {
                                app.adsConsent.pauseForAudienceChoice()
                                choosingAge = true
                            },
                            manageAdsPrivacy = { app.adsConsent.showPrivacyOptions(this@MainActivity) },
                            revokeAppBlocking = { app.disclosures.revokeAccessibility() },
                            revokeVpn = {
                                app.protection.stop()
                                app.disclosures.revokeVpn()
                            },
                        )
                    }
                    composable("privacy") {
                        com.websiteblocker.app.privacy.PrivacyPolicyScreen(back = { nav.popBackStack() })
                    }
                    composable(
                        "rule/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: 0L
                        val vm: RuleViewModel =
                            viewModel(
                                factory =
                                    viewModelFactory {
                                        initializer {
                                            RuleViewModel(
                                                id,
                                                app.repository,
                                                app.protection,
                                                createSavedStateHandle(),
                                            )
                                        }
                                    }
                            )
                        val state by vm.state.collectAsStateWithLifecycle()
                        RuleScreen(
                            state = state,
                            editing = id != 0L,
                            vm = vm,
                            enableProtection = { explanation = true },
                            back = { nav.popBackStack() },
                        )
                    }
                }
                if (choosingAge)
                    com.websiteblocker.app.privacy.AgeGroupDialog(
                        select = {
                            app.adsConsent.pauseForAudienceChoice()
                            val unchanged = app.audience.state.value == it
                            app.audience.setAgeGroup(it)
                            choosingAge = false
                            // Re-selecting the same group also restarts a paused consent check.
                            if (unchanged) app.adsConsent.request(this@MainActivity)
                        },
                        privacy = {
                            app.audience.setAgeGroup(com.websiteblocker.app.privacy.AgeGroup.UNSPECIFIED)
                            choosingAge = false
                            nav.navigate("privacy")
                        },
                    )
                if (appBlockingExplanation)
                    com.websiteblocker.app.ui.permission.AppBlockingExplanation(
                        onDismiss = { appBlockingExplanation = false },
                        onEnable = {
                            appBlockingExplanation = false
                            app.disclosures.acceptAccessibility()
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onPrivacy = {
                            appBlockingExplanation = false
                            nav.navigate("privacy")
                        },
                        onAppInfo = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:$packageName")))
                        },
                    )
                if (explanation)
                    PermissionExplanation(
                        onDismiss = { explanation = false },
                        onPrivacy = { explanation = false; nav.navigate("privacy") },
                        onEnable = {
                            explanation = false
                            app.disclosures.acceptVpn()
                            try {
                                val intent = VpnService.prepare(this)
                                if (intent == null) start() else consent.launch(intent)
                            } catch (_: RuntimeException) {
                                app.protection.failed(
                                    "VPN is unavailable on this device or restricted by its administrator."
                                )
                            }
                        },
                    )
            }
        }
    }
}
