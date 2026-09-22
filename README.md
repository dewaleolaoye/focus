# Focus

A native Android focus tool that stores schedules locally, blocks selected apps from staying in the foreground through Android Accessibility and filters matching website DNS requests simultaneously in both editions. Kotlin, Compose Material 3, Navigation Compose, ViewModel/StateFlow, Coroutines and Room. Minimum Android 8.0 (API 26).

## Build and install

Open this directory in a current stable Android Studio. In **Settings → Build, Execution, Deployment → Build Tools → Gradle**, select Studio's bundled JDK (17 or newer). Install the platform requested by Gradle (Android 37.2) and Build Tools 36.0.0 using SDK Manager. Let Gradle sync finish. `local.properties` is machine-specific and intentionally ignored.

This workstation's installed Studio bundles Java 11, which cannot run this toolchain. Verification uses the already installed JDK 17; update Studio to use a compatible bundled JDK. No global Java configuration is changed.

Command-line build on macOS with a current Studio:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleSideloadDebug :app:testSideloadDebugUnitTest :app:lintSideloadDebug
```

For the older Studio installation on this workstation, use the installed compatible JDK:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
./gradlew :app:assembleSideloadDebug :app:testSideloadDebugUnitTest :app:lintSideloadDebug
```

Sideload APK: `app/build/outputs/apk/sideload/debug/app-sideload-debug.apk` (debug signed). This build includes native blocking for Instagram, WhatsApp, Facebook, X, TikTok and YouTube, including the package variants listed in `ServiceCatalog`. No Play Store deployment is required. Both VPN consent and **App blocking** Accessibility access must be enabled for app and website protection together.

Play/internal-testing APK: `app/build/outputs/apk/play/debug/app-play-debug.apk`. It shares the same enforcement code. Release builds require your own signing configuration; no signing keys are included.

### Physical Android phone

1. Enable Developer options by tapping **Build number** seven times, then enable **USB debugging**. Connect your phone over USB and accept its computer authorization prompt.
2. Select the phone in Android Studio and click **Run**, or run:

   ```sh
   "$HOME/Library/Android/sdk/platform-tools/adb" devices -l
   "$HOME/Library/Android/sdk/platform-tools/adb" -s PHONE_SERIAL install -r app/build/outputs/apk/sideload/debug/app-sideload-debug.apk
   "$HOME/Library/Android/sdk/platform-tools/adb" -s PHONE_SERIAL shell am start -n com.usefocus.app/.MainActivity
   ```

3. Tap **Enable Blocking**, read the privacy explanation, then **Agree and continue**, and accept Android's VPN connection request. Allow notifications for a visible persistent status notification.
4. In either edition, open Focus **Settings → Enable app blocking** (or use the setup card on the dashboard), then tap **Agree and open settings**, choose **App blocking** in Android Accessibility settings, read the system disclosure, and explicitly enable it. The service observes only foreground package changes; it cannot retrieve window content. If Android shows **Restricted setting** for the sideloaded APK, open **Settings → Apps → Focus → ⋮ → Allow restricted settings**, then return to Accessibility and enable **App blocking**. Device wording may vary; see [Android’s instructions](https://support.google.com/android/answer/12623953?hl=en).
5. Add X from the popular apps. Select all days, a start before the current local time and an end after it. Save. Check the home screen says **Protection is on** and marks X **Active**.
6. Follow [the physical-device test matrix](docs/MANUAL_TEST_PLAN.md). Both editions return to Home when a scheduled popular app opens, while the VPN continues filtering service and custom website domains.

Do not enable Android's **Block connections without VPN** option: Focus uses split DNS website blocking; it is not a general full-traffic VPN. Always-on VPN is explicitly unsupported in this MVP.

## Architecture

- `data/model`, `data/local`, `data/repository`: Room entity, DAO Flow, database and validated writes. The version 1→2 migration preserves existing custom website schedules while adding an optional service profile ID.
- `domain`: pure hostname normalization and local-time schedule evaluation. Monday is bit 0, Sunday bit 6. Start inclusive, end exclusive; overnight days refer to the start day. Equal times are rejected.
- `domain/ServiceCatalog`: stable popular-service IDs mapped to friendly names, centralized domain bundles and known Android package variants. The UI never exposes this infrastructure detail.
- `data/website`: discovers a custom site's declared favicon directly from that site, falls back to common icon paths, validates and downsamples the image, then caches it locally. Failed lookups use the globe fallback and are retried after a bounded negative-cache period; no third-party favicon service receives the saved domain.
- `accessibility`: both flavors include foreground-package enforcement for popular-service schedules. A selected app opening during quiet hours triggers Home. Rule updates and a 500 ms schedule clock also enforce an already-open app. The master protection switch gates enforcement, including notification stop. Events and rule updates run on the main thread. The dashboard reports missing/disconnected Accessibility access.
- `vpn/DnsPacketHandler`: bounds-checked IPv4/UDP codec, DNS question extraction, failure replies, upstream response validation and IPv4 checksums.
- `vpn/WebsiteBlockVpnService`: DNS-only split tunnel at `10.111.0.2/32` for service domains and custom websites. This route remains active even when all six native app schedules are active. Rules are observed continuously and local time is evaluated for each request.
- `vpn/ProtectionController`: desired protection is persisted separately from actual process-local service status. A new process never reports a stale persisted “on” state. When VPN consent is still valid, one silent recovery attempt replaces the dashboard repair step; permission loss, revocation and repeated startup failures remain explicit user-action states.
- `receiver/BootReceiver`: attempts restoration after boot/app update when protection was enabled. Runtime restrictions and missing consent leave reactivation status instead of crashing.
- `ui`: lifecycle-aware flows, gated first-run setup, compact state-accurate dashboard, installed or bundled app branding, cached website favicons, Settings explainer, SavedStateHandle-backed editor, inline validation, deletion confirmation, 12/24-hour display and light/dark palettes.

## DNS and privacy

The virtual DNS address is the only IPv4 route captured. Native foreground blocking runs independently through Accessibility; app schedules never replace the DNS tunnel. No HTTP/HTTPS proxying, certificates or page inspection is used.

Matching requests receive local NXDOMAIN and are never sent upstream. Allowed requests go to Cloudflare’s public resolver using authenticated DNS-over-HTTPS on an underlying non-VPN network. There is no plaintext fallback; unavailable or invalid HTTPS responses produce SERVFAIL. Focus is a local DNS firewall, not a full-traffic encrypted VPN.

No browsing history, per-query storage, domain logs, account or application-owned backend. Rules, protection settings and versioned disclosure consent stay locally. Backups and device transfer are disabled. External favicon requests, allowed DNS resolution and advertising have different recipients, explained in the in-app privacy policy.

VPN and Accessibility have separate prominent disclosures and affirmative consent. Withdrawing VPN consent stops master protection; withdrawing Accessibility consent stops native enforcement. Existing installs must accept the new disclosures before enforcement resumes.

Focus is intended for adults and teens aged 13+. Where required by local regulations, Google UMP’s privacy check passes before the ad SDK initializes or requests a banner; errors keep ads off. Debug builds use a Google test banner ID.

## Play submission preparation

See [submission checklist and declarations](docs/play/SUBMISSION.md) and [review video scripts](docs/play/REVIEW_VIDEO_SCRIPT.md). Supply publisher identity, public contact and a hosted policy in `release.properties` using the example. The HTML policy is generated from the same text shipped in the app. Upload signing uses environment variables; no private keys are included. `scripts/build-play-release.sh` checks these prerequisites before producing and verifying a signed bundle. A direct Gradle release bundle without signing configuration is an unsigned QA artifact, not an upload-ready release.

## Deliberate MVP limits

- **Encrypted DNS bypasses filtering.** Android Private DNS (DoT), browser Secure DNS/DoH, in-app resolvers, hard-coded IP addresses and explicit DNS servers outside the virtual endpoint are not intercepted. Disable these for a controlled test. This app is a focus aid, not tamper-resistant parental control.
- **App blocking requires user-enabled Accessibility in both editions.** It prevents selected apps from remaining in the foreground, including cached/offline screens. It does not force-stop their background processes, stop background audio or suppress messages/notifications. Android/OEM restrictions, split-screen, picture-in-picture and cloned/work-profile apps need device-specific verification. Foreground tracking uses window-state events without screen-content access. Device-owner provisioning would be required for tamper-resistant package suspension.
- **Sideloaded Accessibility may require restricted-settings consent.** Follow the in-app setup instructions. Focus does not enable this permission itself or bypass Android's consent. A dashboard setup card remains visible for enabled popular-app rules until the service connects.
- **Service endpoints evolve.** Popular profiles bundle known domains locally and package matching is the primary foreground-app control. Shared infrastructure such as Meta CDNs can cause one selected service's website rule to affect related services.
- **DNS caches and open connections remain usable.** Schedule changes affect new lookups immediately, not existing browser/OS cache entries, open HTTP/2 or QUIC sessions, service workers or offline content. Close browsers and allow cached entries to expire when checking boundaries and recovery.
- **UDP DNS only.** TCP DNS fallback is unsupported. Large/truncated DNS replies, fragmented packets and unusual DNS question encodings may fail. DNS responses are capped at 4096 bytes; UDP payloads are returned through an 8192-byte TUN MTU. Compressed query names and non-IN/multiple-question queries are dropped safely. Ordinary A/AAAA queries are supported.
- **One active VPN.** Starting this app may replace another VPN. Another VPN may revoke this app. Managed/always-on VPN policies may prevent startup. The app exposes reactivation status and does not bypass device restrictions.
- **Restart is best effort.** Force stop, manufacturer background controls, lost consent and foreground-service restrictions can require opening the app again. No exact alarms are needed or requested. Locked-device restore before credential unlock is not supported.
- **Local wall time.** Time-zone changes take effect on the next DNS query; the UI refreshes each second while visible. DST repeated minutes repeat the local schedule; skipped minutes do not occur.
- Overlapping rules form a union: disabling/deleting one rule does not unblock a domain if another active rule covers it. No public-suffix database is bundled; validation checks hostname syntax, not registration or ownership.
- Play Console declarations, hosted publisher policy, AdMob privacy message configuration, review videos and Google approval require publisher action. Local tests do not establish Play eligibility or remove Play Protect warnings.

## Tests

```sh
./gradlew :app:testSideloadDebugUnitTest :app:lintSideloadDebug :app:assembleSideloadDebug
./gradlew :app:testPlayDebugUnitTest :app:lintPlayDebug :app:assemblePlayDebug
./gradlew :app:connectedSideloadDebugAndroidTest
```

The default device suite covers Room CRUD/Flow and reopening a file-backed database. The VPN integration test is skipped by default because it changes the test device's VPN state and makes allowed public DNS queries. Run it only on a fresh disposable emulator with no saved user rules:

```sh
./gradlew :app:connectedSideloadDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.vpnIntegration=true
```

It grants VPN consent via the instrumentation shell on that emulator, launches the app, sends real packets through the TUN, asserts local NXDOMAIN for root/subdomain and successful upstream DNS for an unrelated hostname, then tests disable/delete recovery and cleans up its rule. It does not prove Chrome or Samsung Internet behavior on a physical phone.

The opt-in native integration test temporarily disables existing rules and restores them afterward. It exercises the real installed YouTube app (already open, repeated launch, rule disable/re-enable/delete, master stop/start), all six services' DNS domains and a custom website simultaneously. Run only on a test emulator with YouTube installed:

```sh
./gradlew :app:connectedSideloadDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.appBlockingIntegration=true -Pandroid.testInstrumentationRunnerArguments.class=com.usefocus.app.AppBlockingIntegrationTest
```

See [verification results](docs/VERIFICATION.md) and [manual testing](docs/MANUAL_TEST_PLAN.md).

## Official platform references

Requirements checked September 13, 2026, with stable artifact availability verified against Google's SDK/Maven metadata:

- [Android VPN guide](https://developer.android.com/develop/connectivity/vpn)
- [Foreground-service types: systemExempted includes configured VPN apps](https://developer.android.com/develop/background-work/services/fgs/service-types#system-exempted)
- [Foreground-service background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk)
- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)

The manifest declares `BIND_VPN_SERVICE` on the service, the VPN intent filter, the `systemExempted` foreground type and its permission. Foreground startup follows user VPN consent, or attempts permitted restoration. Notification permission denial does not disable the user-initiated VPN, but Android may display its foreground-service notice in the active-apps UI instead of the notification drawer.
