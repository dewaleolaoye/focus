# Physical-device acceptance plan

Record phone model, Android version, app version, browser versions, current time zone and network. Physical Chrome and Samsung Internet tests have not been performed by the automated emulator suite.

## Test conditions

- Use a phone you control. Disconnect other VPNs; if a managed/always-on VPN cannot be stopped, record that protection is unavailable rather than attempting to bypass it.
- Turn Android **Settings → Network & internet / Connections → Private DNS → Off**. Menu paths vary by manufacturer.
- Turn off **Use Secure DNS** in Chrome privacy/security settings, and any Secure DNS/encrypted resolver setting in Samsung Internet if present. Record the actual setting and browser version.
- Start clean browser sessions, clear site/cache data if appropriate, and close existing tabs/connections. Clearing data can remove logins; use a test browser profile. DNS/connection caching can survive a simple reload. Do not clear personal data merely to force a result.
- For the Play/internal-testing edition, enable **App blocking** under Android Accessibility settings. Confirm its system description states that it cannot retrieve screen content. Return to the app and verify the protection card reports app blocking access as on. The sideload edition must not offer or appear in Accessibility settings.
- For the sideload edition, force-stop the selected popular app before each package-blocking test. Opening the app may still reveal cached/offline screens; verify that feeds, media, messages and refreshes cannot make a network connection on both Wi-Fi and mobile data.
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
- Add each popular service. The editor must show Instagram, WhatsApp, Facebook and X as selectable cards without displaying technical domains. The saved rule must show the service name and icon.
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
- In the Play/internal-testing edition, with active Instagram, WhatsApp, Facebook and X schedules, launch each installed app variant. Expect Android to return to Home immediately and show a short quiet-hours message. Test WhatsApp Business/Facebook Lite too when installed.
- In the sideload edition, create an active rule for each installed popular app, force-stop it, then launch it. Expect the UI to open but all fresh network activity to fail. Confirm unrelated apps retain internet access. Disable the rule and allow up to one second for the VPN to reconfigure; fresh traffic must recover.
- In the Play/internal-testing edition, disable App blocking in Accessibility settings and return to the app. The protection card must report access as off; website DNS blocking must continue independently. Re-enable it and repeat one app launch.
- Confirm custom website rules do not block an unrelated installed app merely because its foreground package changed.
- Enable Private DNS or browser Secure DNS deliberately: record bypass as a documented limitation. Restore original phone settings at the end.

## Release acceptance

Do not mark physical-device acceptance complete until all four browser/network cells have evidence, restart/revocation states have been checked, and the exact tested APK/version is recorded. Emulator raw-DNS success alone does not establish cross-browser behavior on a phone.
