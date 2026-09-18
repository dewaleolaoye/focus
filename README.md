# Focus

A native Android focus tool that stores schedules locally, blocks installed social apps through package-scoped VPN routing, filters matching website DNS requests, and adds optional foreground enforcement in the Play edition. Kotlin, Compose Material 3, Navigation Compose, ViewModel/StateFlow, Coroutines and Room. Minimum Android 8.0 (API 26).

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

Sideload APK: `app/build/outputs/apk/sideload/debug/app-sideload-debug.apk` (debug signed). During an active popular-service schedule, its package-scoped VPN drops all IPv4 and IPv6 network traffic from installed variants of that app. It deliberately omits Accessibility access so Play Protect does not classify a locally shared build as an unverified app requesting sensitive access.

Play/internal-testing APK: `app/build/outputs/apk/play/debug/app-play-debug.apk`. It includes foreground package enforcement through Accessibility and should be distributed through a verified Google Play testing track, not shared as an unverified APK. Release builds require your own signing configuration; no signing keys are included.

### Physical Android phone

1. Enable Developer options by tapping **Build number** seven times, then enable **USB debugging**. Connect your phone over USB and accept its computer authorization prompt.
2. Select the phone in Android Studio and click **Run**, or run:

   ```sh
   "$HOME/Library/Android/sdk/platform-tools/adb" devices -l
   "$HOME/Library/Android/sdk/platform-tools/adb" -s PHONE_SERIAL install -r app/build/outputs/apk/sideload/debug/app-sideload-debug.apk
   "$HOME/Library/Android/sdk/platform-tools/adb" -s PHONE_SERIAL shell am start -n com.websiteblocker.app/.MainActivity
   ```

3. Tap **Enable Blocking**, read the privacy explanation, then **Continue**, and accept Android's VPN connection request. Allow notifications for a visible persistent status notification.
4. In the Play/internal-testing edition only, tap **Enable app blocking**, choose **App blocking** in Android Accessibility settings, read the system disclosure, and explicitly enable it. The service observes only foreground package changes; it cannot retrieve window content.
5. Add X from **Popular services**. Select all days, a start before the current local time and an end after it. Save. Check the home screen says **Protection is on** and shows X in the bento grid.
6. Follow [the physical-device test matrix](docs/MANUAL_TEST_PLAN.md). Foreground package enforcement applies only to the Play/internal-testing edition; the sideload edition relies on bundled DNS targets.

Do not enable Android's **Block connections without VPN** option: Focus switches between package-scoped app blocking and split DNS website blocking; it is not a general full-traffic VPN. Always-on VPN is explicitly unsupported in this MVP.

## Architecture

- `data/model`, `data/local`, `data/repository`: Room entity, DAO Flow, database and validated writes. The version 1→2 migration preserves existing custom website schedules while adding an optional service profile ID.
- `domain`: pure hostname normalization and local-time schedule evaluation. Monday is bit 0, Sunday bit 6. Start inclusive, end exclusive; overnight days refer to the start day. Equal times are rejected.
- `domain/ServiceCatalog`: stable popular-service IDs mapped to friendly names, centralized domain bundles and known Android package variants. The UI never exposes this infrastructure detail.
- `accessibility`: the `play` flavor adds optional foreground-package enforcement for popular-service schedules. When a selected app opens during quiet hours, the service returns to Home. The `sideload` flavor does not compile or declare this service.
- `vpn/DnsPacketHandler`: bounds-checked IPv4/UDP codec, DNS question extraction, failure replies, upstream response validation and IPv4 checksums.
- `vpn/WebsiteBlockVpnService`: switches between two VPN policies as schedules change. Popular-service schedules create a package allowlist with full IPv4/IPv6 routes and discard every packet from the selected installed apps. When no installed-app schedule is active, the service uses the DNS-only split tunnel at `10.111.0.2/32` for custom website filtering. Rules and local time are observed continuously and policy changes apply within about one second.
- `vpn/ProtectionController`: desired protection is persisted separately from actual process-local service status. A new process never reports a stale persisted “on” state. Stop, consent denial, revocation and startup failures have explicit states.
- `receiver/BootReceiver`: attempts restoration after boot/app update when protection was enabled. Runtime restrictions and missing consent leave reactivation status instead of crashing.
- `ui`: lifecycle-aware flows, gated first-run setup, bento home dashboard, Settings explainer, SavedStateHandle-backed editor, inline validation, deletion confirmation, 12/24-hour display and light/dark palettes.

## DNS and privacy

When no installed-app schedule is active, the virtual DNS address is the only IPv4 route captured. During an active popular-app schedule, only the selected installed packages are routed through a full IPv4/IPv6 blackhole tunnel. No HTTP/HTTPS proxying, certificates or page inspection is used.

Matching requests receive local NXDOMAIN and are never sent upstream. Allowed requests use DNS servers from the selected underlying network's LinkProperties. Validated foreground networks are preferred, and another reported underlying network can be tried if socket access fails. Only when none is usable is Cloudflare's public `1.1.1.1` used; this is a public resolver, not an application-owned server. If configured resolvers time out, the service returns SERVFAIL instead of silently switching to a third-party resolver. Wi-Fi/mobile transitions are handled by looking up the underlying network and its DNS servers for each request.

No browsing history, per-query storage, domain logs, analytics, advertising, account or remote backend. Only user-entered rules and the protection setting are persisted. Legacy backup is disabled and modern backup rules exclude all domains from cloud backup and device transfer. Allowed DNS lookups are visible to the selected network DNS resolver in the same way ordinary unencrypted DNS is.

## Deliberate MVP limits

- **Encrypted DNS bypasses filtering.** Android Private DNS (DoT), browser Secure DNS/DoH, in-app resolvers, hard-coded IP addresses and explicit DNS servers outside the virtual endpoint are not intercepted. Disable these for a controlled test. This app is a focus aid, not tamper-resistant parental control.
- **Play-edition UI blocking is user-controlled.** Android Accessibility access can be disabled at any time. It prevents scheduled popular apps from remaining in the foreground, but does not force-stop their background processes or suppress notifications. Device-owner provisioning would be required for tamper-resistant package suspension.
- **The sideload edition blocks app networking, not its launcher UI.** A blocked app can still open and show cached/offline content, but new IPv4/IPv6 connections and existing tunneled traffic receive no response. Force-stop the target app before acceptance testing so an old screen is not mistaken for live access.
- **One VPN policy at a time.** When at least one installed popular app has an active schedule, package-level app blocking takes priority. Custom website DNS rules resume automatically when no installed popular-app schedule is active. Simultaneous per-package full-tunnel blocking and device-wide DNS filtering would require a complete userspace TCP/UDP forwarding stack.
- **Service endpoints evolve.** Popular profiles bundle known domains locally and package matching is the primary foreground-app control. Shared infrastructure such as Meta CDNs can cause one selected service's website rule to affect related services.
- **DNS caches and open connections remain usable.** Schedule changes affect new lookups immediately, not existing browser/OS cache entries, open HTTP/2 or QUIC sessions, service workers or offline content. Close browsers and allow cached entries to expire when checking boundaries and recovery.
- **UDP DNS only.** TCP DNS fallback is unsupported. Large/truncated DNS replies, fragmented packets and unusual DNS question encodings may fail. DNS responses are capped at 4096 bytes; UDP payloads are returned through an 8192-byte TUN MTU. Compressed query names and non-IN/multiple-question queries are dropped safely. Ordinary A/AAAA queries are supported.
- **One active VPN.** Starting this app may replace another VPN. Another VPN may revoke this app. Managed/always-on VPN policies may prevent startup. The app exposes reactivation status and does not bypass device restrictions.
- **Restart is best effort.** Force stop, manufacturer background controls, lost consent and foreground-service restrictions can require opening the app again. No exact alarms are needed or requested. Locked-device restore before credential unlock is not supported.
- **Local wall time.** Time-zone changes take effect on the next DNS query; the UI refreshes each second while visible. DST repeated minutes repeat the local schedule; skipped minutes do not occur.
- Overlapping rules form a union: disabling/deleting one rule does not unblock a domain if another active rule covers it. No public-suffix database is bundled; validation checks hostname syntax, not registration or ownership.
- Release signing and Play Store submission are outside this local MVP. A Play submission must accurately declare VPN use and foreground-service types.

## Tests

```sh
./gradlew :app:testSideloadDebugUnitTest :app:lintSideloadDebug :app:assembleSideloadDebug
./gradlew :app:testPlayDebugUnitTest :app:lintPlayDebug :app:assemblePlayDebug
./gradlew :app:connectedDebugAndroidTest
```

The default device suite covers Room CRUD/Flow and reopening a file-backed database. The VPN integration test is skipped by default because it changes the test device's VPN state and makes allowed public DNS queries. Run it only on a fresh disposable emulator with no saved user rules:

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.vpnIntegration=true
```

It grants VPN consent via the instrumentation shell on that emulator, launches the app, sends real packets through the TUN, asserts local NXDOMAIN for root/subdomain and successful upstream DNS for an unrelated hostname, then tests disable/delete recovery and cleans up its rule. It does not prove Chrome or Samsung Internet behavior on a physical phone.

See [verification results](docs/VERIFICATION.md) and [manual testing](docs/MANUAL_TEST_PLAN.md).

## Official platform references

Requirements checked September 13, 2026, with stable artifact availability verified against Google's SDK/Maven metadata:

- [Android VPN guide](https://developer.android.com/develop/connectivity/vpn)
- [Foreground-service types: systemExempted includes configured VPN apps](https://developer.android.com/develop/background-work/services/fgs/service-types#system-exempted)
- [Foreground-service background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk)
- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)

The manifest declares `BIND_VPN_SERVICE` on the service, the VPN intent filter, the `systemExempted` foreground type and its permission. Foreground startup follows user VPN consent, or attempts permitted restoration. Notification permission denial does not disable the user-initiated VPN, but Android may display its foreground-service notice in the active-apps UI instead of the notification drawer.
