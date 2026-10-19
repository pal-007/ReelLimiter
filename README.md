# ReelLimiter

An Android app that helps you cut down on doom scrolling. You set a daily limit for
Instagram Reels and YouTube Shorts. ReelLimiter counts every reel you watch and, once you hit
your limit, sends you back to the home screen with a "limit reached" message. Counts reset at midnight.

## Features

- Separate daily limits for Instagram Reels and YouTube Shorts (5 to 200, set with a slider)
- Live count and progress bar for each app
- Master on/off switch and a "reset today's counts" button
- Blocks on your next swipe after the limit, and blocks immediately if you reopen Reels/Shorts
- Fully offline. No internet permission, no data leaves the device

## Run it

1. Install [Android Studio](https://developer.android.com/studio) (Koala or newer) and open the `ReelLimiter` folder.
2. Let Gradle sync. Android Studio fetches the wrapper and dependencies.
3. Run on a phone or emulator (Android 8.0 / API 26 or higher).
4. Open the app, tap **Open accessibility settings**, find **ReelLimiter** and switch it on.
5. Set your limits and open Instagram or YouTube.

On some phones (Xiaomi, Oppo, Vivo, Samsung with "sleeping apps") also turn off battery
optimization for ReelLimiter so the service is not killed in the background.

If Android says "Restricted setting" when enabling the service for a sideloaded build: open
**Settings > Apps > ReelLimiter > three-dot menu > Allow restricted settings**, then try again.

## How it works

| File | Role |
|------|------|
| `ReelCounterService.kt` | Accessibility service. Detects Reels/Shorts screens, counts each swipe (debounced), triggers the block |
| `ReelDetector.kt` | Decides if the foreground screen is a Reels/Shorts player (by view ID, with a tab-selected fallback for Instagram) |
| `LimitStore.kt` | Limits, daily counts and midnight rollover in SharedPreferences |
| `MainActivity.kt` | Settings UI: status, sliders, live counts |
| `BlockActivity.kt` | The "limit reached" screen |

The service is restricted to `com.instagram.android` and `com.google.android.youtube` in
`res/xml/accessibility_service_config.xml`, and it never reads text or messages. It only checks
whether a Reels/Shorts screen is open.

## Known limitations

- **View IDs change.** Instagram and YouTube rename internal IDs in updates. If counting stops after an
  app update, update the ID lists in `ReelDetector.kt` (use Android Studio's Layout Inspector or `uiautomatorviewer` to find the new ones).
- **Counting is approximate.** A scroll gesture on the Reels screen (for example scrolling comments) can occasionally count as a reel.
- **Easy to bypass on purpose.** The user can disable the service or the master switch. For a tougher version, add a PIN or a "wait 30 seconds" friction before the switch can be turned off.
- **Play Store:** apps that use the Accessibility API must complete Google's declaration form and explain the use in the listing. Fine for personal sideloading.

## Ideas for next steps

- Time limit (minutes) in addition to a count limit
- Weekly stats chart and streaks
- Cool-down: allow 5 more reels after a 60-second pause
- Add TikTok, Snapchat Spotlight or Facebook Reels through a new `TrackedApp` entry
