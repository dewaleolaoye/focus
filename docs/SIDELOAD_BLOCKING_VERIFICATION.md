# Sideload app blocking — September 19, 2026

Latest privacy, audience and encrypted-resolver changes: [release preparation verification](play/VERIFICATION.md). Earlier results below describe the previous build.

This replaces the distribution/enforcement description in the September 18 verification report.

## Fix

The previous sideload build set `ACCESSIBILITY_APP_BLOCKING=false` and did not package the native enforcement service. Its current VPN captured only DNS, so an installed social app could remain usable through cached content, existing connections or its own resolver.

Both distributions now package the same Accessibility service. With the user-granted permission, it returns to Home when an active popular-app rule matches the foreground package. It rechecks rules immediately and wall time every 500 ms, including while an app is already open. The master switch and the notification stop action disable native enforcement as well as the website VPN.

The DNS-only VPN remains active alongside native enforcement. App rules never replace website filtering. App blocking does not require a Play Store release.

The dashboard shows missing/disconnected app-blocking access and offers setup instructions, including Android's restricted-settings flow for sideloaded APKs. APK inspection confirms that the service cannot retrieve window content.

## Build checks

- 52 sideload unit tests passed, including all six service IDs/package variants, exact package matching, disabled rules, master-off, start/end boundaries, overnight rules and overlapping rules.
- Sideload lint passed with no errors. An existing Android 12-only notification call is now guarded for Android 8–11 compatibility.
- Both sideload-debug and play-debug compiled. The final sideload APK and instrumentation APK were rebuilt and installed on the connected Android emulator.
- Visually checked Settings, the Enable app blocking button, and the permission explanation: instructions and both settings links are visible and readable on the emulator.
- Packaged sideload manifest contains `AppBlockAccessibilityService`; generated BuildConfig enables it; packaged XML has `canRetrieveWindowContent=false`.

## Live emulator integration — passed

Android 16 / API 36 (`emulator-5554`), final sideload-debug APK. `AppBlockingIntegrationTest.nativeBlockingAndWebsiteFilteringWorkTogether`: **OK (1 test)**, 96.563 seconds.

Assertions covered:

- The real installed YouTube app is usable with no active rule.
- Adding an active YouTube rule while it is open returns to the actual Home launcher.
- Three subsequent user-style launches are blocked. Launches use the instrumentation shell, avoiding Android's restrictions on background app-initiated activity starts.
- All six services' primary domains and `www` subdomains return NXDOMAIN concurrently with native blocking.
- A custom website rule also returns NXDOMAIN; an unrelated domain resolves through upstream DNS.
- Disabling the YouTube rule restores app access and DNS. Re-enabling ejects the already-open app.
- The notification STOP command clears the persisted master switch and lets YouTube remain open while Accessibility stays enabled.
- Restarting protection restores native enforcement.
- Deleting the YouTube rule restores native access and website DNS.
- A future YouTube schedule begins while the app is open, returns to Home at the boundary, and blocks its website DNS.

The test restores existing schedule enabled states, removes its temporary rules, and restores Accessibility, protection and YouTube notification permission settings. The other five real social apps were not installed, so their package selection is unit-tested rather than claimed as live-device coverage.

## Installation and device acceptance

Install `app/build/outputs/apk/sideload/debug/app-sideload-debug.apk` over the existing debug build. Keep VPN protection enabled. Open Focus Settings → Enable app blocking → Open Accessibility settings → App blocking → On.

If Android shows Restricted setting, open Focus app info → three-dot menu → Allow restricted settings, then enable App blocking. See [Android's official instructions](https://support.google.com/android/answer/12623953?hl=en).

Only an emulator was connected. Real Instagram, WhatsApp, Facebook, X and TikTok launches on the user's phone remain unverified. Physical browser/network coverage, device-specific battery restrictions, split-screen/PiP and cloned/work-profile variants remain in `MANUAL_TEST_PLAN.md`. Native enforcement prevents foreground use; it does not force-stop background processes, audio or notifications. Existing DNS limitations remain unchanged.

APK SHA-256:

```
a4d0f6a04ad2854ca054e1f64309e9a0c539baa30cf058c8a5e9d16cd736e4d2
```
