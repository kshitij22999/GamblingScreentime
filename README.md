# Time Gambler

Time Gambler is a React Native Android app with a Kotlin usage monitor and app blocker. The Android project and its npm package are in `TimeGamblerApp/`.

## Development

Install the JavaScript dependencies and Android SDK, connect an Android device with USB debugging enabled, then run:

```sh
cd TimeGamblerApp
npm install
npm run android
```

For Android unit tests:

```sh
cd TimeGamblerApp/android
./gradlew :app:testDebugUnitTest
```

## Release signing

Release APK/AAB tasks require a private upload key. Keep the keystore outside the repository and provide these values through your local environment or CI secret store:

- `TIME_GAMBLER_UPLOAD_STORE_FILE`
- `TIME_GAMBLER_UPLOAD_STORE_PASSWORD`
- `TIME_GAMBLER_UPLOAD_KEY_ALIAS`
- `TIME_GAMBLER_UPLOAD_KEY_PASSWORD`

Then build the Play upload bundle with:

```sh
cd TimeGamblerApp/android
./gradlew :app:bundleRelease
```

The output is `app/build/outputs/bundle/release/app-release.aab`. Do not commit the keystore or passwords. Configure Play App Signing in Play Console and keep a secure backup of the upload key.

## Google Play readiness

The current Android build targets API 33 and uses an older React Native/Android Gradle toolchain. Update and validate the toolchain and target SDK to the Play requirement before submission; do not only change the target number without testing Android's newer foreground-service and background-start restrictions.

The app's onboarding now shows an in-app Accessibility Service disclosure and requires affirmative consent before opening Accessibility settings. The corresponding Play Console declaration and privacy policy/Data safety answers still need to be completed and reviewed for accuracy before publishing.
