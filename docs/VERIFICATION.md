# Verification — September 18, 2026

## Implemented scope

Focus now has two distribution variants and a redesigned native Compose experience:

- `sideload`: package-scoped VPN app blocking without Accessibility, intended for direct debug installation.
- `play`: adds opt-in Accessibility foreground enforcement and is intended for verified Play/internal testing.
- First-run users see only the blocking setup; schedule creation appears after VPN protection is enabled.
- The enabled home uses the selected bento design, authentic installed-app launcher icons, a dedicated Settings explainer and a new adaptive launcher mark.

The app-blocking and DNS paths are real Android VPN behavior, not a UI-only prototype. Physical-device acceptance remains required for OEM and real social-app behavior.

## Environment

- macOS arm64 with JDK 17.
- Gradle 9.6.0, AGP 9.4.0, compile SDK 37.2, target SDK 37, minimum SDK 26.
- Visual QA device: `Pixel_API_36`, Android 16/API 36, 1440 × 3120 px at 560 dpi (`411 × 891 dp`).
- No physical Android phone was connected during this pass.

## Final results

| Check | Result |
| --- | --- |
| Sideload unit tests | 33 passed, 0 failures |
| Play unit tests | 33 passed, 0 failures |
| Sideload lint | Passed |
| Play lint | Passed |
| Sideload APK | Built and installed successfully on the emulator |
| Play APK | Built successfully |
| Variant manifest split | Sideload contains VPN service and no Accessibility service; Play contains both |
| First-run gate | Verified: `Enable Blocking` is visible and `Add schedule` is absent |
| Enabled dashboard | Verified with four active service schedules; all four bento cards and action are visible |
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
3c1c7ea991a16a37c15993b497dd69766da5978c07260a05a401053b9d1a388f  app-sideload-debug.apk
2ca8e30cd352132e58672c0c65ea6f79de61e9df2d86bd10ecb3b031ecc65224  app-play-debug.apk
```

Build reports and APKs are generated files. Re-run the final command after a clean checkout to reproduce them.
