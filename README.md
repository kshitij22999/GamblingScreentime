Time Gambler - Phase 1

This commit implements Phase 1 vertical slice for Time Gambler.

Files created (high level):
- package.json, tsconfig.json, app.json, index.js
- src/: App.tsx, theme.ts, screens (Dashboard, AppSelection, TimerConfig), components (AppCard), services (nativeBridge)
- android/: minimal Kotlin native module bridge and service stubs

Architecture:
- React Native (TypeScript) UI
- Kotlin native module (TimeGambler) exposed to RN via NativeModules
- AsyncStorage used for UI persistence

Android permissions used (Phase1):
- None explicitly requested in this skeleton. Usage Access will be requested later when monitoring is implemented.

Bridge:
- TimeGamblerModule.kt exposes getInstalledApps(), startMonitoring(), stopMonitoring().
- React Native calls NativeModules.TimeGambler and listens to DeviceEventEmitter events.

Persistence:
- Protected apps are stored in AsyncStorage under key 'protectedApps'.
- Enforcement-critical state will be persisted on the Kotlin side (DataStore/SharedPreferences) in later phases.

Limitations / Known issues:
- Android Gradle project scaffolding not included; to build run 'npx react-native init' or integrate these files into a React Native project.
- Native module is a stub; getInstalledApps returns a small list. Foreground monitoring service is a stub.

Build / Run instructions (developer):
1. Install Node.js, yarn/npm, Android SDK, Java JDK.
2. Run: npm install
3. Use 'npx react-native run-android' from project root (after completing Android native scaffolding). See official React Native docs.

Next steps:
- Implement real installed-apps discovery using PackageManager in the Kotlin module.
- Implement Usage Access permission flow and a foreground monitor using UsageStatsManager or AccessibilityService as needed.
- Persist enforcement state in SharedPreferences or DataStore and implement a robust bridge.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
