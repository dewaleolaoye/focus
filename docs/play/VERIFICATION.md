# Privacy and release-preparation verification — 19 September 2026

## Implemented

- Separate versioned, affirmative VPN and Accessibility disclosures, policy entry points and withdrawal controls. Android permission alone is insufficient. Native event handling discards foreground observations when disclosure consent is absent.
- Full bundled policy with matching generated public HTML. The public HTML remains explicitly marked as a draft until the publisher supplies identity, public email and hosting URL.
- Optional local age group for the owner-specified adult/teen audience. Under-18/unspecified users receive no ad requests or UMP consent-form requests. Adults require a successful current UMP check; SDK initialization and banner requests are gated. Debug banners use Google's test ID.
- Allowed DNS forwarded using certificate-verified HTTPS to Cloudflare, with no UDP/plaintext fallback. Response validation rejects invalid, oversized or redirected replies. Website icons require HTTPS; application cleartext traffic is disabled.
- Environment-based upload signing and a release preflight that fails on absent publisher metadata, signing prerequisites, mismatched hosted policy or an unsigned bundle. No key or identity was invented.

## Automated checks

- Sideload: 60 unit tests passed; lint passed with no errors (17 non-blocking warnings).
- Play: 60 unit tests passed; release lint passed with no errors (17 warnings). Final optimized bundle built successfully; ZIP inspection confirms it has no upload signature and is only an unsigned QA artifact. Its bundled policy matches the source.
- Shell syntax, Python syntax and git whitespace checks passed.
- Missing-metadata preflight correctly rejected release preparation without overwriting the visibly marked policy draft.
- The age-choice screen was inspected on the API 36 emulator; selecting teen persisted only an enum group, without a date of birth.

## Remaining external verification

No physical phone was connected. All six native apps, OEM background behavior, network transitions, split-screen and picture-in-picture require the device matrix in ../MANUAL_TEST_PLAN.md. Emulator DNS packet tests do not prove every browser/cache configuration.

AdMob account-side regional forms and consent geography tests are pending configuration. Publisher name, contact email, hosted policy and upload credentials are still owner-supplied prerequisites. Play Console declarations, actual review videos, signed upload, account settings and Google's approval are not completed by this local build. See SUBMISSION.md and REVIEW_VIDEO_SCRIPT.md. Nothing in these changes guarantees removal of a Play Protect installation warning.

## Sideload artifact

`app/build/outputs/apk/sideload/debug/app-sideload-debug.apk`, version 1.3.0 (4), debug signed.
SHA-256: `4007ca25ad7b0c4bec466c53a5e6c5db765cc28b15a657c174fb10ccb7f1a740`.
The APK's bundled policy matches `app/src/main/assets/privacy-policy.txt` exactly.
