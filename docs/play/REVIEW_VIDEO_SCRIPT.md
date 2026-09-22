# Actual review-video recording plan

Record the final release UI on a test phone/emulator. Keep the VPN demonstration at or below 90 seconds per Google's VPN declaration guidance. A separate Accessibility video can show the additional steps. Do not demonstrate consent by shell-granting permissions; reviewers need the user-visible path. Use a real installed app and a schedule active at recording time. Avoid showing personal messages/accounts or live-ad clicks.

## Accessibility (separate recording)

1. Open Focus. Show an enabled YouTube schedule and the App blocking needs setup card.
2. Tap Enable app blocking. Pause/scroll slowly so the complete separate disclosure is readable. It explains package-name access, purpose, no history/sharing, and Home enforcement.
3. Tap Not now. Show that the app remains usable and the setup card still offers the feature.
4. Reopen the disclosure. Tap Agree and open settings. Enable App blocking through Android's normal Accessibility UI, accepting its system confirmation.
5. Return to Focus. Show the active schedule. Launch YouTube from the launcher; show the return to Home and quiet-hours toast. Avoid an authenticated account if possible.
6. In Focus Settings, withdraw app blocking consent. Launch YouTube again to show it can remain open. Also show the privacy policy entry point.

Suggested narration: "Focus only reads the foreground package name. When my chosen schedule is active, it returns me to Home. The service cannot retrieve screen content. I can refuse or withdraw access. No Accessibility data is shared with advertising."

## VPN (at most 90 seconds)

1. Open Focus with VPN off. Tap Enable Blocking. Slowly show the full DNS disclosure, including Cloudflare, domain names/IP, encryption, local blocking and withdrawal.
2. Decline once. Reopen, accept the disclosure and accept Android's VPN system dialog.
3. Show an active website rule and the VPN notification. In a clean test browser with browser Secure DNS disabled, demonstrate a known blocked domain failing and an unrelated site working. Avoid cached tabs/connections and explain this test condition.
4. Stop protection from its notification or withdraw consent from Settings. Show the status is off.

Suggested narration: "This uses VpnService as an on-device DNS firewall. Blocked queries stay local; allowed DNS goes to Cloudflare over HTTPS. It is not a full browsing VPN. The developer does not receive query history, and VPN traffic is never used for ads."

Upload the actual recordings to reviewer-accessible URLs and verify that a signed-out browser can play them. Enter these URLs in the respective declaration forms. These instructions are not themselves a submitted video or a claim of Google approval.
