# Focus — Play submission package

This package prepares the current implementation for review. Google determines eligibility and approval; neither a passing build nor a signed bundle constitutes approval. Do not upload the debug APK as the production release.

## Owner-supplied items still required

1. Copy `release.properties.example` to `release.properties`. Supply the actual developer/company name matching the listing, monitored public privacy/support email and intended HTTPS privacy-policy URL. Do not use example identities.
2. Run `python3 scripts/prepare-play-submission.py --draft` to review the public HTML generated from the **same policy text embedded in the app**. Remove draft status only after the real identity is supplied by running `python3 scripts/prepare-play-submission.py` (it renders the final policy before checking signing/hosting). Publish `docs/play/privacy-policy.html` to the supplied URL. It must be public, readable without login, non-geofenced and remain available. Do not submit the visibly marked draft.
3. Supply your existing upload key using `FOCUS_KEYSTORE_PATH`, `FOCUS_KEYSTORE_PASSWORD`, `FOCUS_KEY_ALIAS`, `FOCUS_KEY_PASSWORD` in the local environment. Do not commit a keystore/password or paste passwords into chat. If this app already exists in Play, use its registered upload key (or the Play key-reset procedure), not a newly invented key. New apps can use a newly generated upload key with Play App Signing; retain secure backups.
4. In AdMob, configure and publish the applicable Privacy & messaging forms for the existing app ID, including required regional consent/privacy options. Verify the ad account's selected partners and geography. The code pauses ads if UMP setup or consent checks fail; that failure does not prevent blocking. A code integration cannot publish these account-side messages.
5. The owner specified **adults and teenagers**: select the applicable 13–15, 16–17 and 18+ target-audience groups in Play Console, not under-13 groups. The code stores a neutral age-group choice locally and requests ads/UMP consent only for adults (18+). Under-18 and unspecified groups get no ad requests. Verify the country-specific audience/content-rating questions and actual marketing. Do not claim parental-control or disability-assistance functionality.
6. Complete Accessibility, VPN, foreground service and Advertising ID declarations where requested, plus Data Safety. Use the drafts below and verify them against the live Console questions and account configuration.

## Build and verify

Use JDK 17+ (`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home` on the current workstation).

```
./scripts/build-play-release.sh
```

This checks publisher metadata, required signing environment variables, and that the public HTTPS policy exactly matches the generated policy. It runs Play release unit tests/lint and builds `app/build/outputs/bundle/playRelease/app-play-release.aab`, then verifies its signature. With no credentials, a direct Gradle bundle task can produce an **unsigned QA artifact**; it is not upload-ready. The script is the submission preflight and fails on missing prerequisites.

Keep the AdMob app/ad unit IDs already configured for release. Debug builds use Google's test banner ID. Do not click live ads during testing. Version code is 4 / version name 1.3.0; confirm code 4 is greater than any version already uploaded before using it.

## Store listing draft

Title: **Focus: App & Website Blocker**

Short description: **Schedule breaks from distracting apps and websites with on-device controls.**

Full description:

Focus helps you schedule quiet hours for Instagram, WhatsApp, Facebook, X, TikTok, YouTube and selected websites. Choose days and times, edit or pause rules, and turn protection off whenever you need to.

**App blocking — Accessibility permission**
Focus uses Android's AccessibilityService API, with your explicit consent, to identify the foreground app by package name and return to Home when one of your app schedules is active. It cannot retrieve screen content and does not read messages, passwords, typed text or screenshots. Foreground app observations are not recorded or shared with advertisers. This is deterministic enforcement of schedules you create, not autonomous decision-making. You can decline or withdraw access.

**Website blocking — VPN permission**
Focus uses Android's VpnService API as a local DNS firewall to reject website lookups that match your schedules. The tunnel stays on your device. Allowed DNS requests go to Cloudflare's public resolver over HTTPS; blocked queries stay local. This is not an anonymity VPN and does not proxy or encrypt all browsing traffic. Focus does not use VPN traffic to monetize other apps' traffic, redirect ads or alter advertising location.

**Your choices and limits**
Schedules remain on your device. Ads are provided by Google AdMob after applicable privacy checks; advertising privacy choices and the full privacy policy are available in Settings. Android permits one VPN at a time. Encrypted DNS in other apps, existing connections and caches can bypass DNS filtering. Accessibility blocks foreground use; it does not suspend packages, stop background audio or suppress notifications. Android/OEM restrictions can require reactivation. This is a voluntary focus tool, not tamper-proof parental control.

## Accessibility declaration draft

- Accessibility tool / primary disability assistance: **No** (`isAccessibilityTool=false`).
- Core feature: Return to Android Home when the foreground package matches a user-created active blocking schedule.
- Why needed: Android's normal application APIs cannot remove another user's selected app from the foreground when a schedule triggers. The service observes window-state events and performs only the Home action; it cannot retrieve window content.
- Data accessed: Foreground application package name, processed transiently on device. No accessibility-derived data is uploaded, logged, used by AdMob or retained as app-usage history.
- User control: Separate disclosure explains data, purpose and lack of sharing; **Agree and open settings** records versioned consent and opens system settings. **Not now** leaves access unaccepted. Android permission alone does not start enforcement without the in-app consent. Consent can be withdrawn from Focus Settings independently of the VPN.
- Automation: Fixed user-defined trigger/action (active schedule + selected app -> Home). No planning, autonomous decisions, deceptive UI interaction, changing settings, uninstall prevention or privacy-control bypass.
- Video: Follow `REVIEW_VIDEO_SCRIPT.md`; upload the resulting real demonstration to an accessible review URL and enter that URL in Console. A script/document is not a substitute for the required video.

## VPN declaration draft

- Is a conventional remote VPN the core feature? **No**. Describe the on-device DNS firewall explicitly.
- Closest permitted category to substantiate: **Device security / firewall**, based on filtering user-selected DNS traffic. Focus is marketed for productivity, so explain its actual firewall function and let Google assess fit. Do not claim antivirus, parental control, enterprise management or app-usage analytics that the code does not provide.
- Technical explanation: VpnService creates a local DNS-only TUN at 10.111.0.2/32. It rejects scheduled domains with local NXDOMAIN. Allowed DNS wire messages are POSTed over HTTPS to https://1.1.1.1/dns-query or https://1.0.0.1/dns-query on a non-VPN underlying network. Default TLS certificate/hostname verification is retained; redirects, oversized/invalid responses and plaintext fallbacks are rejected. No remote full-traffic VPN tunnel exists.
- Data: Domain/DNS request processed locally; allowed DNS queries and IP address received by Cloudflare for resolution. No developer-side query logs or browsing database. Cloudflare's provider policy applies. This disclosure is presented before VPN consent, and material disclosure changes require renewed in-app consent.
- Monetization: AdMob banners are independent of VPN/Accessibility data. Focus never redirects other apps' ads or monetizes their traffic.
- If the reviewer finds this use outside permitted categories, resolve that review with Google; do not silently relabel the product or assume encryption alone grants eligibility.

## Data Safety working answers

The table is a **review draft**, not a ready-to-submit import. Google's definitions include third-party SDK behavior and data transmitted off device even when the developer does not store it. Do not select blanket "No data collected" while using AdMob or forwarding DNS.

| Data/source | Actual behavior | Declaration handling |
| --- | --- | --- |
| Schedules, rules, selected age group, consent versions/times | On-device database/preferences only; backups disabled | Not off-device collection by Focus. Explain local retention/deletion in policy. |
| Accessibility package name | Transient memory, never uploaded/history-logged | On-device processing; no Accessibility-derived sharing/advertising. |
| Allowed DNS / Cloudflare | Domain requests and IP address transmitted for resolution via HTTPS | Assess browsing-history and approximate-location/IP categories under current Console definitions. Functionality purpose; optional because VPN can be declined. Verify provider processing/retention before claiming an ephemeral or service-provider exemption. |
| Website/icon host requests | HTTPS homepage/icon retrieval exposes selected hostname/path and IP to the website/icon provider | Account for this feature when answering browsing/IP collection questions; no separate favicon-provider database. |
| Google AdMob | IP/approximate location, device or other identifiers, app interactions, diagnostics/performance as documented by Google | Include applicable collection/sharing and advertising, analytics/measurement, fraud/security purposes for the configured SDK/partners. Do not assume denying personalized ads eliminates all collection. |
| UMP consent processing | Google determines required privacy choices and stores applicable consent choices | Include SDK behavior and keep AdMob Privacy & messaging configuration consistent. |
| Account information | No account creation | No account-deletion flow required for a nonexistent account; local deletion instructions and public contact are supplied. |

Reference Google's current [SDK data disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure). Confirm security answers using the final SDK/partner list and traffic inspection; the app's own outbound DNS/icon traffic uses HTTPS. Avoid asserting an unverified provider retention period.

## Required verification before rollout

- Test consent accepted, declined, unavailable/offline and subsequently changed, including applicable EEA/UK/Swiss and other regional messages with a configured AdMob account. Check that banners disappear while privacy choices are being changed and stay absent when checks fail. Core blocking must still work.
- Verify fresh install and upgrade both require the new VPN/Accessibility in-app consent. Decline, restart, re-enter and withdraw each independently.
- Repeat native and website blocking on the real phone, including network changes and all six installed packages. See `../MANUAL_TEST_PLAN.md`.
- Review the merged release manifest/SDK reports, release signing, current target API requirements and any Play Console warnings for the actual uploaded bundle.
- Start with an internal or closed testing release and install **from Google Play**. Uploading a bundle does not guarantee removal of a sideload Play Protect warning or policy approval.

## Official references

- https://support.google.com/googleplay/android-developer/answer/10964491 (Accessibility)
- https://support.google.com/googleplay/android-developer/answer/12564964 (VpnService)
- https://support.google.com/googleplay/android-developer/answer/10144311 (User Data/privacy policy)
- https://developers.google.com/admob/android/privacy (UMP)
- https://developers.google.com/admob/android/privacy/play-data-disclosure (AdMob data)
- https://developers.cloudflare.com/1.1.1.1/encryption/dns-over-https/make-api-requests/ (encrypted resolver)
