# Physical-device acceptance plan

Record phone model, Android version, app version, browser versions, current time zone and network. Physical Chrome and Samsung Internet tests have not been performed by the automated emulator suite.

## Test conditions

- Use a phone you control. Disconnect other VPNs; if a managed/always-on VPN cannot be stopped, record that protection is unavailable rather than attempting to bypass it.
- Turn Android **Settings → Network & internet / Connections → Private DNS → Off**. Menu paths vary by manufacturer.
- Turn off **Use Secure DNS** in Chrome privacy/security settings, and any Secure DNS/encrypted resolver setting in Samsung Internet if present. Record the actual setting and browser version.
- Start clean browser sessions, clear site/cache data if appropriate, and close existing tabs/connections. Clearing data can remove logins; use a test browser profile. DNS/connection caching can survive a simple reload. Do not clear personal data merely to force a result.
- In **sideload-debug** as well as Play builds, enable **App blocking** under Android Accessibility settings. If Android shows **Restricted setting**, use Focus app info → three-dot menu → **Allow restricted settings**, then enable the service. Confirm the service cannot retrieve screen content. Return to Focus and verify the setup warning disappears.
- Native enforcement returns to Home; background messages/audio are not force-stopped. No Play Store upload is required.
- Choose start = a few minutes before now, end = several minutes after now, all days. Verify protection is on and the schedule is active. Tests crossing midnight should select the correct start day.

## Browser/network matrix

Run the full sequence separately in each cell:

| Browser | Wi-Fi (mobile data off) | Mobile data (Wi-Fi off) |
| --- | --- | --- |
| Chrome | Pending physical test | Pending physical test |
| Samsung Internet | Pending physical test | Pending physical test |

For each cell:

1. With protection off, verify `https://x.com` and `https://example.com` load on this network. Record any region/network/site issue before testing the app.
2. Enable protection. Add the currently active `x.com` rule. Visit `x.com` in a fresh tab: expect a DNS/name-resolution failure, not a custom block page.
3. Test `www.x.com` and a subdomain known to resolve on this network before blocking. Expect DNS failure. A subdomain that never resolved is not evidence of blocking.
4. Visit unrelated sites (`example.com` plus another known working site): expect ordinary browsing. Confirm a nonmatching suffix such as `notx.com` is not treated as blocked; its own existence/HTTP behavior is independent. Hostname-boundary behavior is also unit-tested.
5. Toggle the rule off. Open a clean session after cache expiry: expect `x.com` to resolve again. Toggle on: expect blocking of new lookups again.
6. Edit the rule to a future time: expect new lookups to resolve. Edit back to an active period: expect DNS failure.
7. Delete the rule, cancel once to confirm it remains, then confirm deletion. Expect access to return after any negative cache expires.
8. Recreate the rule. Close and reopen the app: expect the same domain, times, selected days and enabled state. A normal app close should leave the notification/VPN active.
9. While protection is active, switch from Wi-Fi to mobile data and back. After each transition, verify unrelated domains resolve and blocked domains do not. Repeat with temporarily unavailable internet; expect no app crash, then recovery once connected.

## Schedule and validation

- Same-day 09:00–17:00: verify start inclusive and end exclusive with fresh DNS requests.
- Monday 22:00–Tuesday 07:00, only Monday selected: Monday night and Tuesday before 07:00 blocked; Monday before 07:00 and Tuesday night available. Automated evaluator tests cover this without changing the real clock.
- Sunday overnight must carry into Monday. Every-day shortcut selects seven days.
- Zero selected days, equal times and invalid hostnames must show inline errors without saving.
- Paste `https://WWW.X.COM:443/home?source=test#part`: saved display must be `x.com`.
- Add each popular service. The editor must show Instagram, WhatsApp, Facebook, X, TikTok and YouTube as selectable cards without displaying technical domains. The saved rule must show the service name and icon.
- Edit a popular-service rule, switch it to **Custom website**, and verify an empty website field appears. Save a valid custom domain and confirm it displays as a website rule.
- Select a different device time zone: the same local schedule should follow that zone on subsequent requests. Restore the original zone after testing.
- Two overlapping rules: disabling one must leave the other effective.

## Lifecycle, consent and accessibility

- Decline VPN consent: no crash; clear not-protected/reactivation message. Retry and accept.
- Deny notification permission: no crash; explain that Android may place service status in the active-apps UI. Grant permission and confirm a persistent notification and working **Turn off** action.
- Start a second VPN: this app must stop/report reactivation. Return and re-enable if device policy permits.
- Revoke VPN permission in system settings: no crash; no false “on” status.
- Restart phone with protection enabled: after unlocking, verify automatic restoration if allowed. If denied by Android/OEM, opening the app must show reactivation, and manual enable must recover.
- Force stop the app: expect protection to stop and reopening to request reactivation. Do not report force-stop survival as supported.
- Airplane mode then reconnect: no crash or retained dead resolver socket.
- Rotate during editing, background/reopen editor, switch dark mode, use 200% font size and TalkBack. Check website labels, rule switches, all weekday chips, validation, time pickers and save/back controls are reachable.
- In **sideload-debug**, create active schedules for all six apps. Launch each from its icon, Recents, a deep link and a notification. Expect Home and a quiet-hours message. Repeat rapid launches; check Lite/Business/regional variants when installed. Repeat in Play debug if distributing that flavor.
- Leave each app open before the schedule starts. Expect Home within about 500 ms of the boundary, without touching the screen. At the end, confirm the app can open normally. Test time-zone change and overnight schedules.
- While all six schedules are active, verify all six websites and an active custom website rule are still blocked; unrelated websites/apps remain usable.
- Disable, delete or move a rule outside its schedule; app access must return. Overlapping rules must continue blocking until the last active rule ends.
- Turn off protection from Settings and separately from the VPN notification. Both native enforcement and website filtering must stop. Re-enable and test both again.
- Disable App blocking in Accessibility settings and return to Focus. The dashboard must show **App blocking needs setup** for enabled popular-app rules; website DNS blocking continues. Re-enable and repeat a launch.
- Test split-screen, picture-in-picture, screen lock/unlock, battery saver and manufacturer background restrictions on the actual phone. Record any exception rather than treating emulator coverage as device acceptance.
- Confirm custom website rules do not block an unrelated installed app merely because its foreground package changed.
- Enable Private DNS or browser Secure DNS deliberately: record bypass as a documented limitation. Restore original phone settings at the end.

## Release acceptance

Do not mark physical-device acceptance complete until all four browser/network cells have evidence, restart/revocation states have been checked, and the exact tested APK/version is recorded. Emulator raw-DNS success alone does not establish cross-browser behavior on a phone.


## Privacy and audience regression (1.3.0)

- Fresh install and upgrade: decline the separate VPN and Accessibility disclosures; confirm neither permission is treated as in-app consent. Accept separately and confirm both controls work. Withdraw app-blocking consent: native app opens, website DNS still blocks. Withdraw website consent: master protection and both controls stop.
- For ad tests use Google's test ad IDs and configured UMP test geography/devices. Test consent allowed, declined, no-form-required, offline and retry. An unavailable consent service must keep ads off without preventing blocking.
- Open the full privacy policy from first-run, each disclosure and Settings. Compare publisher identity/contact and public policy to the final release configuration.
- Verify allowed DNS over HTTPS, resolver outage returning failure without plaintext fallback, Wi-Fi/cellular transitions, and private-network hostname limitations. Re-run all six apps and browser sites on a physical phone.
