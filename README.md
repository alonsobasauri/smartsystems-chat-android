# SmartSystems Chat - Android App

Android application that wraps the SmartSystems Chat PWA in a native WebView with offline detection and automatic reconnection.

## Features

- **Full-screen WebView** - Native app experience for the PWA
- **Automatic offline detection** - Monitors network connectivity in real-time
- **Custom offline page** - Beautiful offline screen with auto-retry functionality
- **Auto-update system** - Automatically checks for new releases from GitHub and prompts to download/install
- **WebView state persistence** - Saves and restores WebView state on rotation/recreation
- **Back button navigation** - Native back button works within WebView history
- **Hardware acceleration** - Optimized performance with GPU rendering
- **PWA support** - Full support for Progressive Web App features including:
  - Service Workers
  - Local Storage
  - IndexedDB
  - Cache API
  - JavaScript

## Technical Stack

- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: 28 (Android 9.0 Pie)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with Kotlin Coroutines and Flow

## Project Structure

```
android-app/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/com/smartsystems/chatapp/
│   │       │   ├── MainActivity.kt              # Main WebView activity
│   │       │   ├── network/
│   │       │   │   └── NetworkMonitor.kt        # Network connectivity monitor
│   │       │   └── webview/
│   │       │       └── WebViewConfig.kt         # WebView configuration
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml        # Main layout
│   │       │   ├── values/
│   │       │   │   ├── strings.xml              # String resources
│   │       │   │   ├── colors.xml               # Color palette
│   │       │   │   └── themes.xml               # App theme
│   │       │   └── xml/
│   │       │       ├── backup_rules.xml         # Backup configuration
│   │       │       └── data_extraction_rules.xml
│   │       ├── assets/
│   │       │   └── offline.html                 # Offline page
│   │       └── AndroidManifest.xml              # App manifest
│   ├── build.gradle.kts                         # App-level build config
│   └── proguard-rules.pro                       # ProGuard rules
├── build.gradle.kts                             # Project-level build config
├── settings.gradle.kts                          # Project settings
├── gradle.properties                            # Gradle properties
└── README.md                                    # This file
```

## Key Components

### NetworkMonitor.kt
- Uses `ConnectivityManager.NetworkCallback` for real-time network monitoring
- Exposes network state via Kotlin `StateFlow`
- Validates internet connectivity (not just network availability)
- Lifecycle-aware with proper cleanup

### WebViewConfig.kt
- Configures WebView for optimal PWA support
- Enables JavaScript, DOM Storage, and Database
- Configures cache settings for offline support
- Security settings (mixed content, file access)
- Enables WebView debugging in debug builds

### MainActivity.kt
- Manages WebView lifecycle
- Observes network state changes with Kotlin Flow
- Automatically switches between online/offline views
- Handles back button navigation
- Saves/restores WebView state

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 17 or newer
- **Android SDK** with SDK 34 (Android 14)
- **Gradle** 8.2 or newer (included via wrapper)

## Building the App

### 1. Open Project in Android Studio

```bash
cd /home/alonso/Documents/apo/android-app
# Open this directory in Android Studio
```

Or from command line:
```bash
studio /home/alonso/Documents/apo/android-app
```

### 2. Sync Gradle

Android Studio will automatically prompt to sync Gradle. If not:
- Click **File > Sync Project with Gradle Files**

### 3. Build Debug APK

**From Android Studio:**
- Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**
- APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

**From Command Line:**
```bash
cd /home/alonso/Documents/apo/android-app
./gradlew assembleDebug
```

### 4. Build Release APK (Signed)

**Step 1: Generate Keystore** (first time only)
```bash
keytool -genkey -v -keystore smartsystems-chat.keystore \
  -alias smartsystems -keyalg RSA -keysize 2048 -validity 10000
```

**Step 2: Create signing config**

Edit `app/build.gradle.kts` and add before `buildTypes`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../smartsystems-chat.keystore")
        storePassword = "YOUR_STORE_PASSWORD"
        keyAlias = "smartsystems"
        keyPassword = "YOUR_KEY_PASSWORD"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... rest of config
    }
}
```

**Step 3: Build Release APK**
```bash
./gradlew assembleRelease
```

APK will be at: `app/build/outputs/apk/release/app-release.apk`

## Installing the App

### Install on Physical Device (via USB)

```bash
# Enable USB debugging on your device first
adb install app/build/outputs/apk/debug/app-debug.apk

# Or for release:
adb install app/build/outputs/apk/release/app-release.apk
```

### Install on Emulator

```bash
# Start emulator from Android Studio or:
emulator -avd YOUR_AVD_NAME

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install via Android Studio

1. Connect device or start emulator
2. Click **Run > Run 'app'** or press Shift+F10
3. Select target device

## Configuration

### Change PWA URL

Edit `MainActivity.kt:31`:
```kotlin
private const val PWA_URL = "https://chat.smartsystems.work/"
```

### Customize App Name

Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Your App Name</string>
```

### Customize Colors

Edit `app/src/main/res/values/colors.xml` and `themes.xml`

### Change Package Name

If you need to change from `com.smartsystems.chatapp`:

1. Rename package in Android Studio (Right-click package > Refactor > Rename)
2. Update `namespace` in `app/build.gradle.kts`
3. Update `applicationId` in `app/build.gradle.kts`
4. Update `package` in `AndroidManifest.xml`

## Versioning

Version is controlled in `app/build.gradle.kts`:

```kotlin
versionCode = 1        // Integer, increment for each release
versionName = "1.0.0"  // String, user-facing version
```

## Debugging

### Enable WebView Debugging

WebView debugging is automatically enabled in debug builds. To inspect:

1. Connect device via USB
2. Open Chrome and navigate to: `chrome://inspect`
3. Find your WebView and click "inspect"

### View Logs

```bash
# Filter by app package
adb logcat | grep "com.smartsystems.chatapp"

# Or use Android Studio Logcat window
```

## Deployment

### Google Play Store

1. Build signed release APK (see above)
2. Create app listing at [Google Play Console](https://play.google.com/console)
3. Upload APK or AAB (App Bundle recommended)
4. Complete store listing
5. Submit for review

### AAB (App Bundle) - Recommended for Play Store

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Direct Distribution (APK)

For direct distribution outside Play Store:

1. Build release APK
2. Host APK on your server
3. Users must enable "Install from Unknown Sources"
4. Share APK download link

## Troubleshooting

### Gradle Sync Failed

```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### WebView Not Loading

- Check internet permissions in `AndroidManifest.xml`
- Verify PWA URL is accessible
- Check WebView debugging for JavaScript errors

### App Crashes on Start

```bash
# Check crash logs
adb logcat | grep "AndroidRuntime"
```

### Offline Page Not Showing

- Verify `offline.html` exists in `app/src/main/assets/`
- Check NetworkMonitor is properly detecting network state
- Look for errors in logcat

## Performance Optimization

### Enable ProGuard for Release

Already configured in `app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(...)
    }
}
```

### Reduce APK Size

1. Use App Bundle (AAB) instead of APK
2. Enable ProGuard/R8 (already enabled)
3. Remove unused resources:
```kotlin
buildTypes {
    release {
        isShrinkResources = true
    }
}
```

## Security Considerations

- **HTTPS Only**: App only allows `smartsystems.work` domain
- **Mixed Content**: Blocked via `MIXED_CONTENT_NEVER_ALLOW`
- **File Access**: Disabled for security
- **Safe Browsing**: Enabled
- **JavaScript**: Enabled (required for PWA) - ensure PWA is secure

## License

Proprietary - SmartSystems

## Support

For issues or questions, contact: [your-contact-info]

## Auto-Update System

The app includes an automatic update system that:
- Checks GitHub releases every 6 hours for new versions
- Compares version codes to determine if update is needed
- Shows a dialog with release notes when update is available
- Downloads APK using Android DownloadManager
- Prompts user to install the update automatically

### How It Works

1. App checks `https://api.github.com/repos/alonsobasauri/smartsystems-chat-android/releases/latest`
2. Compares `versionCode` from release tag with installed version
3. If newer version found, shows update dialog
4. User clicks "Descargar" to download APK
5. After download completes, installation prompt appears automatically

### Creating Releases

To create a new release that triggers auto-update:

```bash
# 1. Update version in app/build.gradle.kts
versionCode = 2
versionName = "1.0.1"

# 2. Build release APK
./gradlew assembleRelease

# 3. Create GitHub release
gh release create v1.0.1 \
  app/build/outputs/apk/release/app-release.apk \
  --title "Version 1.0.1" \
  --notes "Bug fixes and improvements"
```

The version tag must follow semantic versioning (e.g., `v1.0.0`, `v1.2.3`).

## Changelog

### Version 1.0.0 (2025-01-08)
- Initial release
- Full-screen WebView with PWA support
- Automatic offline detection
- Custom offline page with auto-retry
- Network state monitoring
- Back button navigation
- GitHub auto-update system
