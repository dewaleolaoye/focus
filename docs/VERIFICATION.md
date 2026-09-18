# Verification — September 18, 2026

## Implemented scope

Focus now has two distribution variants and a redesigned native Compose experience:

- `sideload`: package-scoped VPN app blocking without Accessibility, intended for direct debug installation.
- `play`: adds opt-in Accessibility foreground enforcement and is intended for verified Play/internal testing.
- First-run users see only the blocking setup; schedule creation appears after VPN protection is enabled.
- The enabled home uses compact status cards and schedule rows, authentic app branding, a dedicated Settings explainer and the Focus launcher mark.

The app-blocking and DNS paths are real Android VPN behavior, not a UI-only prototype. Physical-device acceptance remains required for OEM and real social-app behavior.

## Environment

- macOS arm64 with JDK 17.
- Gradle 9.6.0, AGP 9.4.0, compile SDK 37.2, target SDK 37, minimum SDK 26.
- Visual QA device: `Pixel_API_36`, Android 16/API 36, 1080 × 1920 px for this pass.
- No physical Android phone was connected during this pass.

## Final results

| Check | Result |
| --- | --- |
| Sideload unit tests | 48 passed, 0 failures |
| Play unit tests | 48 passed, 0 failures |
| Sideload lint | Passed |
| Play lint | Passed |
| Sideload APK | Built and installed successfully on the emulator |
| Play APK | Built successfully |
| Variant manifest split | Sideload contains VPN service and no Accessibility service; Play contains both |
| First-run gate | Verified: `Enable Blocking` is visible and `Add schedule` is absent |
| Dashboard states | Verified empty, upcoming, active, protection-off and starting/attention derivation |
| Schedule management | Verified create, edit, delete confirmation, enable state and immediate dashboard updates |
| Keyboard behavior | Verified URL input stays visible and Save schedule remains reachable with the IME open |
| App and website icons | Verified proper Instagram fallback plus direct-site favicon discovery, caching and globe fallback |
| Settings flow | Verified: protection state, blocking explanation, privacy and VPN note render and scroll |
| Selected-state copy | Verified: no visible `Selected` label in the rule picker |
| Product Design QA | Passed; see `../design-qa.md` |
| Physical Instagram/Facebook/WhatsApp/X test | Pending; follow `MANUAL_TEST_PLAN.md` |

Final command:

```sh
./gradlew :app:testSideloadDebugUnitTest :app:testPlayDebugUnitTest \
  :app:lintSideloadDebug :app:lintPlayDebug \
  :app:assembleSideloadDebug :app:assemblePlayDebug
```

## Artifacts

- Sideload APK: `../app/build/outputs/apk/sideload/debug/app-sideload-debug.apk`
- Play/internal-testing APK: `../app/build/outputs/apk/play/debug/app-play-debug.apk`
- Sideload lint: `../app/build/reports/lint-results-sideloadDebug.html`
- Play lint: `../app/build/reports/lint-results-playDebug.html`
- Sideload unit report: `../app/build/reports/tests/testSideloadDebugUnitTest/index.html`
- Play unit report: `../app/build/reports/tests/testPlayDebugUnitTest/index.html`
- Screenshots: `screenshots/onboarding.png`, `screenshots/home.png`, `screenshots/settings.png`

SHA-256:

```text
2948ce9f452fe61eb3ee267c7c72d2eda9723d45cb90fd9f674aa5048cd589b9  app-sideload-debug.apk
0a5cf2e492cf335ec9cdf3570514398c4024eca778bef819078e8923635bf084  app-play-debug.apk
```

Build reports and APKs are generated files. Re-run the final command after a clean checkout to reproduce them.
