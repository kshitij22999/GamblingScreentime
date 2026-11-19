# Time Gambler — Project Rules, Aim, and Progress

## Project aim

Build a native Android screen-time management application with a React Native UI and a Kotlin Android enforcement layer.

Local testing target:
- Use Android Studio to run and validate the app locally on an Android emulator or connected device.
- The app should be tested in an Android Studio emulator during local development and verification.
- This is the preferred local runtime environment for validating the Android bridge, foreground monitoring, and timer logic.

### Android Studio local setup checklist

1. Open the React Native Android project in Android Studio by selecting the `TimeGamblerApp/android` directory (or open the project root and select the Android configuration when prompted).
2. Install the Android SDK platform and build tools required by the project Gradle configuration. Install any requested SDK components through Android Studio's SDK Manager.
3. Configure a JDK supported by the project's Android Gradle Plugin; use JDK 17 if compatible with the checked-in Gradle and plugin versions.
4. Let Gradle sync complete and resolve any missing SDK or dependency prompts.
5. Create or select an Android Virtual Device (AVD), preferably a portrait phone profile, from Device Manager. Alternatively, connect a developer-enabled Android device.
6. Start the emulator and wait for it to finish booting. Grant Usage Access manually in Android settings when that permission flow is implemented.
7. Start Metro from the project root with `npm start`, then run the `app` configuration in Android Studio, or use `npx react-native run-android` from the project root.
8. Test app discovery, timer configuration, bridge communication, foreground-only countdown, background/screen-lock behavior, and process/reboot persistence as those features become available.
9. Check Android Studio's Run, Logcat, and Build output when investigating native or Gradle failures. Record device/API-level-specific limitations in project documentation.

Core product concept:
- Each installed app gets its own independent daily time budget.
- Time runs only while that app is the real foreground app.
- Users can gamble remaining time in a simplified Teen Patti-style game.
- Losing stakes removes time; winning doubles the stake up to a max balance cap.
- The app must be primarily Android phone UI, in portrait orientation, with a Balatro-inspired dark card-game aesthetic without copying copyrighted assets.

Important architectural rule:
- React Native is the UI layer only.
- Kotlin is the enforcement engine.
- Enforcement must continue even if the RN process dies or the app is backgrounded.

## Core product rules

### Timer behavior
- Each protected app has a daily limit and remaining time.
- Time decreases only when that app is the active foreground app.
- Time does not decrease while:
  - the screen is locked;
  - another app is in the foreground;
  - the home screen is visible;
  - settings are open;
  - the Time Gambler app itself is open.
- The Time Gambler app itself never consumes time.

### Multiple timers
- Each app has its own timer.
- There is no shared time wallet between apps.
- No transfer between apps is allowed.

### Time configuration
- Users can add installed apps and assign daily time limits in minutes.
- Sensible increments should be used such as 5-minute steps.
- The user cannot manually add extra time once active.
- Users can change or cancel a limit at any time (product decision, 2026-10-11; this replaced the original once-per-24h rule). Each change or cancel asks for confirmation in a playful dialog.

### Timer persistence
- Timer state must survive React Native reload, app restarts, Android process death, and reboot.
- Enforcement-critical state should live in Kotlin persistence (SharedPreferences / DataStore / SQLite), not only JS memory.
- Kotlin should be the source of truth for remaining-time enforcement.

### Timer expiration and blocking
- When a timer reaches zero, normal use of that app must be blocked.
- Blocking must happen through a native Android mechanism, not just a JS screen.
- The blocker should trigger when the app is launched.
- The user must not be able to simply hit Back to continue using the blocked app.

### Gambling rules
- Minimum bet: 5 minutes
- Maximum bet: 6 hours
- Bet increments: 5 minutes
- User may bet the entire remaining balance
- Stake is deducted immediately
- Win: stake doubles, then balance is capped at 24 hours
- Loss: stake is lost
- Tie: stake returned
- Game never creates time from nowhere
- User cannot gamble below 5 minutes remaining
 

### Maximum balance cap
- V1 maximum balance per app is 24 hours.
- If a win would exceed 24 hours, cap at 24 hours.

### Teen Patti game requirements
- Simplified automatic Teen Patti game only.
- Game flow: choose app, choose bet, confirm, stake deducted, deal 3 cards to player and opponent, evaluate hands, compare, result, payout.
- Standard simplified ranking order:
  1. Trail / Three of a Kind
  2. Pure Sequence
  3. Sequence
  4. Color
  5. Pair
  6. High Card
- Use deterministic hand evaluation logic with tests.
- Use secure randomness, not Math.random().
- Deck generation must produce 52 unique cards, shuffled, and no duplicate cards.
- Make the gambling engine use UsageStatsManager from Android to detect past week app usage data and have a dynamic engine with a difficulty curve based on the user's usage patterns.

### Crash / interruption handling
- If the app is closed or the process is killed mid-game, rewards must not be duplicated.
- Use explicit state progression and persistence.
- State can be modeled like:a
  - GAME_CREATED
  - STAKE_RESERVED
  - CARDS_GENERATED
  - RESULT_FINALIZED
  - BALANCE_COMMITTED
- Only finalized games should change final balances.

### Local-only V1
- No backend required.
- No Firebase, Supabase, remote APIs, login, or accounts.
- Everything must work locally.

### Android permission constraints
- Only request genuinely required permissions.
- The first-run onboarding should explain why each permission is necessary.
- Relevant permissions may include Usage Access, foreground service, overlay permission if needed, boot completed if needed, and package discovery attributes where required.
- This must be documented and justified.

### Security / bypass considerations
- The app acknowledges that no Android app can provide absolute tamper-proof protection.
- Reasonable casual bypasses should be considered: time changes, timezone changes, app reinstalls, force stops, notifications/deep links, split-screen, and picture-in-picture.
- Platform-dependent enforcement code must be isolated and upgradeable.

## Development phases mandated by the product brief

Phase 1
- React Native app setup
- TypeScript
- Kotlin native bridge
- portrait-only orientation
- navigation
- basic Balatro-inspired theme

Phase 2
- Installed app discovery

Phase 3
- Timer engine

Phase 4
- Persistent local state

Phase 5
- Native foreground app monitoring

Phase 6
- Blocking screen

Phase 7
- Timer configuration and 24-hour change rule

Phase 8
- Teen Patti engine

Phase 9
- Teen Patti UI and animations

Phase 10
- Crash recovery / transaction handling

Phase 11
- Security and bypass testing

Phase 12
- UI polish

## First required task
The brief explicitly says: do not implement everything at once. Start with a minimal vertical slice and only then continue.

Required initial vertical slice:
1. React Native launches.
2. Kotlin bridge works.
3. Installed apps can be retrieved.
4. User can select an app.
5. User can assign a timer.
6. Kotlin can detect the foreground app.
7. Timer decreases only while that app is active.
8. React Native dashboard reflects remaining time.

Only after that works should the blocking mechanism and Teen Patti game be layered on top.

## Work completed till now

### Environment setup
- Created a project directory and initialized a React Native TypeScript app scaffold with the React Native CLI.
- Installed project dependencies and configured navigation + local storage.
- Configured Android project files and Kotlin support for the native module bridge.

### React Native UI progress
- Created a basic dark Balatro-inspired dashboard screen.
- Added app selection screen.
- Added timer configuration screen.
- Added persistent storage logic with AsyncStorage.
- Added a native bridge wrapper for RN to call Kotlin module methods.

### Kotlin / Android progress
- Added a native module named TimeGambler.
- Added a foreground monitor service stub.
- Added package registration to MainApplication.
- Added Android manifest permissions and portrait mode lock.
- Added a minimal timer service logic and Kotlin side timer state for a single active app.

### Baseline build (done 2026-10-10)
- Builds with JDK 17, Node 18, Gradle 7.5.1 / AGP 7.3.1 and compileSdk 33 (`android/local.properties` points at `~/Android/Sdk`).
- Navigation, screens, safe-area-context and async-storage were pinned back to RN 0.71-compatible versions (v7 / 4.x / 5.x need newer RN).
- Verified on a physical OPPO CPH2423 (Android 15) over USB. The API 37 Play Store emulator needs about 7.4 GB of free disk to boot, which the dev VM doesn't have.
- Device run flow: `npm start` in `TimeGamblerApp/`; `./gradlew assembleDebug`; `adb install -r app/build/outputs/apk/debug/app-debug.apk`; `adb reverse tcp:8081 tcp:8081`; launch the app.
- ColorOS devices: turn on Developer options → "Disable permission monitoring" or adb installs are rejected.

### Vertical slice milestone (done 2026-10-10, needs on-device verification of the countdown)
Architecture decisions:
- **Kotlin is the only store of timer state.** `TimerRepository` keeps each app's budget (`AppBudget`) in SharedPreferences as JSON. AsyncStorage no longer holds any timer state. RN polls `getSnapshot()` once a second while the dashboard is focused.
- **The rules live in a pure engine** (`TimerEngine`, `TimerRules`) with no Android dependencies, covered by JVM unit tests:
  - limits are 5–120 minutes in 5-minute steps;
  - a limit change shifts today's remaining time by the difference and never refills used time;
  - each app's budget resets on a new local calendar day.
- **Foreground detection carries state between polls** (`ForegroundAppDetector`): it tracks MOVE_TO_FOREGROUND/BACKGROUND, SCREEN_NON_INTERACTIVE and KEYGUARD_SHOWN events. The old version only looked back 60 seconds and lost track after a minute in an app.
- **Charging uses the monotonic `elapsedRealtime` clock**, capped at 5s per tick. Nothing is charged while the screen is off or locked, or while this app, a home screen or Settings is in front; those packages can't be protected.
- **Monitoring always runs:** `ForegroundMonitorService` is a foreground service, and `MonitorRestartReceiver` restarts it on BOOT_COMPLETED / MY_PACKAGE_REPLACED. The UI has no way to stop it.
- **The app list comes from launcher intents** with a scoped `<queries>` block, replacing QUERY_ALL_PACKAGES. This also shows preinstalled apps like YouTube and Chrome.
- ~~There is no "remove app" yet.~~ Added 2026-10-11 as "Cancel this limit" (see below).

Permissions (justification):
- PACKAGE_USAGE_STATS: needed to detect the foreground app. The user grants it in Settings; onboarding explains why.
- FOREGROUND_SERVICE: keeps enforcement running while the UI is closed.
- POST_NOTIFICATIONS: shows the ongoing monitoring notification on Android 13+. Optional.
- RECEIVE_BOOT_COMPLETED: resumes monitoring after a reboot.
- Accessibility service (`BlockerService`): required for blocking. It only receives window-change events (`canRetrieveWindowContent=false`), so it knows which app opened but never reads what is on screen. It replaced SYSTEM_ALERT_WINDOW on 2026-10-24.
- INTERNET: only for the RN dev server (Metro). The app itself makes no network calls.

Known gaps / next:
- Wall-clock / timezone tampering still affects the daily reset (Phase 11).
- Split-screen and picture-in-picture are counted as whichever app is resumed (Phase 11).
- targetSdk is 33. Moving to 34+ requires declaring a foreground service type for the monitor.
- The root `src/`, `android/`, `package.json` and `index.js` are an older stale skeleton; the real app is `TimeGamblerApp/`.

### Blocking milestone (done 2026-10-10, verified on device)
- Counting now uses Android's usage-event timestamps (`ForegroundTimeline`), so time is still charged correctly if ColorOS freezes the service. Each charge is capped by uptime (`elapsedRealtime` plus 2s slack) so a forward clock change can't drain hours.
- The ongoing notification shows a live system countdown (chronometer) while a protected app is open.
- `ForegroundMonitorService` polls every 500ms. When the foreground app has no time left, `BlockGate` decides whether to launch the native `BlockerActivity`, throttling repeat launches to one per 1.5s for the same app.
- `BlockerActivity` is plain Kotlin/XML, so it works without React Native. It runs in its own task affinity and is excluded from Recents. Back and "Go to home screen" both go to the launcher, and reopening the app, including from Recents, blocks it again.
- Verified on the OPPO phone: open the exhausted app → blocked; press Back → home screen; reopen → blocked again.
- Limitation: there's up to about 0.5–1s before the block appears. An AccessibilityService would make it instant, at the cost of a heavier permission.

### Instant blocking (2026-10-24; replaces the BlockerActivity approach above)
- The user asked for Digital Wellbeing-style blocking. Wellbeing uses `PackageManager.setPackagesSuspended`, which needs SUSPEND_APPS. Only system apps or the Wellbeing role holder get that permission, and the DevicePolicyManager variant needs device-owner provisioning on a phone with no accounts. **Decision:** use an AccessibilityService, as third-party blockers do.
- `blocking/BlockerService` listens for TYPE_WINDOW_STATE_CHANGED. Only windows whose class is a real activity count as the foreground app, so keyboards, the notification shade and toasts are ignored. If that app's budget is exhausted, `BlockerOverlay` adds a full-screen opaque TYPE_ACCESSIBILITY_OVERLAY window right away. No polling and no activity launch are involved, so the app never gets focus.
- Back and "Go to home screen" both run `GLOBAL_ACTION_HOME`. When any other activity comes forward, or the screen turns off, the overlay is removed. It is re-checked on ACTION_USER_PRESENT.
- `ForegroundMonitorService` still does all timing. Each tick calls `BlockerService.refresh()`, so an app whose time runs out while it's open is covered within 500ms.
- Removed: `BlockerActivity`, `BlockGate` (and its test), `OverlayAccess`, and the SYSTEM_ALERT_WINDOW permission in the main manifest. The debug manifest keeps that permission for RN's dev overlay. The bridge now has `blocking` in `getPermissionStatus` and `openBlockerSettings()`.
- Android 13+ may label the service a "restricted setting" for sideloaded APKs. Onboarding explains the App info › ⋮ › Allow restricted settings step.

### 8-bit UI and snappier steppers (2026-10-24)
- **Look:** every widget is built from `components/PixelFrame` (one-pixel outline, notched corners, optional bevel and hard drop shadow). `PIXEL` = 4dp is the grid unit, defined in `theme.ts`.
- **Fonts:** Press Start 2P (headings, buttons, numbers) and VT323 (body), both SIL OFL. They live in `android/app/src/main/assets/fonts` for RN, and in `res/font` for the native blocker overlay. Changing fonts needs a native rebuild.
- **Components:** `PixelButton` replaced `ChunkyButton` and sinks into its shadow on touch-down. `StepButton` (shared by TimerConfig and TeenPatti) steps on touch-down rather than release, then repeats while held, speeding up from 140ms to 45ms. `PixelMeter` is a segmented power bar for the limit, set by tapping or dragging, one segment per 5 minutes.
- Glyphs missing from the pixel fonts (♠ etc.) fall back to the system font. Plain ASCII `-` is used instead of `−`.

### Teen Patti engine milestone (Phase 8, done 2026-10-10; engine and bridge only, no UI yet)
Architecture decisions:
- **The game runs entirely in Kotlin** (`game/`). RN calls `playTeenPatti(gameId, packageName, stakeMinutes, allIn)` and gets back the finished round. The UI only animates a result that is already saved.
- **Rules and pure logic have no Android dependencies**, so they're covered by JVM tests: `Deck`, `HandEvaluator`, `TeenPattiEngine`, `StakeRules`, `OddsPolicy`.
  - Randomness comes from `SecureRandom` and is passed in as `java.util.Random`, so tests can use a seeded `Random`.
  - Ranking is Trail > Pure Sequence > Sequence > Color > Pair > High Card. A-K-Q is the best sequence, A-2-3 the second best, then K-Q-J down to 2-3-4. Suits never break ties; an exact tie is a push.
  - Fair odds, exact over all 22,100 × 18,424 match-ups: the player wins 49.945%, ties 0.111%. The rules are checked against the published hand frequencies (Wikipedia: 52 / 48 / 720 / 1,096 / 3,744 / 16,440).
  - House rule: a fully equal colour hand is a push. Some tables break it by suit instead.
- **Stakes:** 5 min to 6 h in 5-minute steps, never more than the balance, and no gambling with under 5 min left.
  - All-in bets the exact balance, even when it isn't a 5-minute multiple, capped at the 6 h maximum bet.
  - A win pays 2× the stake, capped at a 24 h balance. A tie returns the stake.
  - Winnings last only for the current day: the daily rollover resets the balance to the limit. A later limit change clamps the balance to the new limit.
- **Usage-based difficulty (`OddsPolicy` + `usage/WeeklyUsage`):**
  - `UsageStatsManager.queryAndAggregateUsageStats` supplies the last 7 days of foreground time for the app being gambled on, plus total screen time (excluding Time Gambler).
  - Pressure (0–1) = 0.7 × ramp(average daily use ÷ daily limit, 0.5 → 2.0) + 0.3 × ramp(the app's share of screen time, 10% → 50%).
  - With no Usage Access or no data, the game is fair.
- **How the light rig works:** cards are always dealt fairly from a real shuffled deck. When the player would win, the house redeals one fresh round with probability `0.5 × pressure` and keeps that result, which the player can still win.
  - The player's win chance is w·(1 − p·(1 − w)), which ranges from 49.9% (fair) down to about 37.5% at worst. A heavy user still wins about 1 in 3 rounds.
  - This is a deliberate product choice: the house never forces a loss, it just leans.
  - The edge is disclosed: `getGambleOdds(packageName)` returns the tier (FAIR / LEANING / TOUGH / HARSH) and win chance so the UI can show it before a bet.
- **Crash safety (pulls part of Phase 10 forward):** `TimerRepository.transact` writes the new balance and the game record in one synchronous SharedPreferences `commit()`.
  - The brief's GAME_CREATED → BALANCE_COMMITTED states therefore happen all at once or not at all. Killing the app mid-game can't skip a loss or duplicate a win.
  - The UI must generate a `gameId` per attempt and reuse it on retry. A repeated id returns the stored result (`replayed: true`).
  - The last 50 games are kept in `GambleLedger`.

Known gaps / next:
- ~~Phase 9 UI~~: first pass done 2026-10-10 (see below).
- Gambling isn't rate-limited yet. Repeated all-ins can drain a balance fast, which is arguably the point.
- **Testing only, remove before release:** a RESET button at the top left of the Dashboard (shown only when `__DEV__` is true) calls `debugResetAll`, which runs `TimerRepository.resetAll` and wipes every budget and game record. Release builds refuse the call. To remove it, delete those three pieces plus the bridge entry and the Jest mock.

### Teen Patti UI, first pass (Phase 9, 2026-10-10)
- Each Dashboard timer card has a **GAMBLE THIS TIME** button. It's disabled when the app has under 5 min left.
- `screens/TeenPatti.tsx`:
  - The balance pill shows the app's remaining time.
  - The odds badge shows the tier, the win %, and why (from `getGambleOdds`).
  - A felt table shows the HOUSE and YOU hands.
  - Bet picker: ±5 min steps, quick chips (5m/15m/30m/1h), and an ALL IN toggle.
  - A Deal / Deal again button.
- **Order is result first, animation second:** `playTeenPatti` has already saved the result before any card moves. The cards then deal alternately, the player's flip, a pause, and the house's flip one by one.
  - Leaving or killing the app mid-animation changes nothing.
  - Back is disabled while dealing.
- **Retries:** the game id stays pending until a call succeeds, so a retry after an unknown error reuses it and can't pay out twice. A rejected bet (`E_STAKE_*`) frees the id.
- Pure picker helpers live in `src/teenPatti.ts`, tested in `__tests__/teenPatti-test.ts`, and mirror `StakeRules`. Kotlin still validates every bet.

### Change or cancel a limit freely (2026-10-11)
- **The 24h change cooldown is gone**, at the user's request. Limits can be changed or cancelled whenever the user likes.
- Changing a limit still shifts today's remaining time by the difference: raising it adds time, lowering it takes time away, and it never resets the whole day.
- Cancelling removes the budget (`TimerRepository.cancelLimit`, bridge `cancelAppLimit`). If the user adds the app again, it starts with a full budget.
- Both actions go through `LimitConfirmDialog`, a themed modal that always shows the same prompt: "Fold your hand?". Only the summary line and the confirm label change between cancelling and changing. Adding a new app needs no confirmation.
- Trade-off: the user can always get out of an exhausted timer this way. The confirmation dialog is the only friction.

## Project structure

React Native app (TimeGamblerApp/)
- src/
  - App.tsx (routes to Onboarding until Usage Access is granted)
  - nativeBridge.ts (typed wrapper; throws if the native module is missing)
  - types.ts, theme.ts, time.ts (formatting), limits.ts (UI mirror of the limit rules)
  - components/ AppCard.tsx, ChunkyButton.tsx, LimitConfirmDialog.tsx
  - screens/ Onboarding.tsx, Dashboard.tsx, AppSelection.tsx, TimerConfig.tsx, TeenPatti.tsx
  - teenPatti.ts (stake picker helpers, labels), components/PlayingCard.tsx (animated card)
  - components/ PixelFrame.tsx, PixelButton.tsx, StepButton.tsx, PixelMeter.tsx (8-bit primitives)
- __tests__/ App-test.tsx (smoke test), time-test.ts
- jest.setup.js (mocks the TimeGambler native module)

Android native (TimeGamblerApp/android/app/src/)
- main/java/com/timegamblerapp/
  - MainActivity.java, MainApplication.java, TimeGamblerPackage.kt
  - TimeGamblerModule.kt (bridge: getInstalledApps, getSnapshot, setAppLimit, cancelAppLimit, getPermissionStatus, openUsageAccessSettings, openBlockerSettings, startMonitoring, getGambleOdds, playTeenPatti)
  - timer/ AppBudget.kt, TimerRules.kt, TimerEngine.kt, DayKeys.kt, TimerRepository.kt
  - monitor/ ForegroundAppDetector.kt, ForegroundMonitorService.kt, MonitorRestartReceiver.kt
  - apps/ LauncherAppCatalog.kt
  - blocking/ BlockerService.kt (accessibility service, config res/xml/blocker_service.xml), BlockerOverlay.kt (layout: res/layout/blocker_overlay.xml)
  - monitor/ ForegroundTimeline.kt (event timeline → usage)
  - permissions/ UsageAccess.kt, BlockerAccess.kt
  - game/ Card.kt, Deck.kt, HandEvaluator.kt, TeenPattiEngine.kt, OddsPolicy.kt, StakeRules.kt, GambleRecord.kt (ledger), TeenPattiTable.kt (orchestration + atomic settle)
  - usage/ WeeklyUsage.kt (7-day UsageStatsManager profile)
- test/java/com/timegamblerapp/ timer/TimerEngineTest.kt, monitor/ForegroundTimelineTest.kt, game/ HandEvaluatorTest, TeenPattiRulesTest (exhaustive, vs published Teen Patti rules), TeenPattiEngineTest, TeenPattiDealTest, OddsPolicyTest, OddsPolicyRulesTest, StakeRulesTest, StakeRulesEdgeTest, GambleLedgerTest, TeenPattiTableTest

Tests: `./gradlew testDebugUnitTest` (Kotlin, in `android/`) and `npx jest` (JS, in `TimeGamblerApp/`).

## Project rules for future work

- Do not make major architecture decisions silently; document them.
- Keep UI and enforcement logic separated.
- Do not put core enforcement only in JavaScript.
- Keep the Kotlin layer authoritative for time state.
- Persist any important state locally and do not rely on memory only.
- Keep the app portrait-only.
- Use modular code; do not build everything in one giant file.
- Prefer explicit state models and enums over scattered booleans.
- Write tests for timer logic and game logic as features are added.
- Keep Android permission requests minimal and justified.
- Use safe, deterministic, and testable logic.
- Preserve the “Balatro-inspired but not copied” visual direction.
- Prefer Android Studio emulator/device testing for local validation of Android behavior and UI flows.
- Prefer Android Studio run/debug workflows over speculative backend or remote validation steps.

## Important note for the next assistant

The next coding agent should continue the work from this exact point, not restart from scratch, and should respect the architecture and rules described here. The work should continue toward the required vertical slice, with Kotlin enforcement and timer logic being hardened and then followed by the later game and blocking features.

### First step before further development

Before implementing new features or expanding the vertical slice, first get the current base project running locally in Android Studio on an emulator or connected Android device. Confirm Gradle sync and build succeed, the app launches, and Metro/React Native loads the UI. Fix any existing setup or build blockers first, and record the working run instructions and any remaining environment limitations. This establishes a reliable baseline and keeps subsequent development and debugging streamlined.

## Co-authored note

This document was created to give the next assistant a clear operational baseline for continuing the project.
