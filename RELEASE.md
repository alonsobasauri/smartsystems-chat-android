# Release Process

This document describes how to create a new release of the SmartSystems Chat Android app.

## Prerequisites

- Android SDK installed
- Java JDK 17 or higher
- GitHub CLI (`gh`) authenticated
- Signing keystore configured (for production releases)

## Release Steps

### 1. Update Version

Edit `app/build.gradle.kts` and increment the version:

```kotlin
versionCode = 2        // Increment this integer
versionName = "1.0.1"  // Update semantic version
```

Version code must be an integer that increases with each release.
Version name should follow semantic versioning: `MAJOR.MINOR.PATCH`

### 2. Update Changelog

Edit `README.md` and add release notes under the Changelog section:

```markdown
### Version 1.0.1 (2025-01-08)
- Bug fixes
- Performance improvements
- New feature X
```

### 3. Build Release APK

**Option A: Using Gradle (unsigned - for testing)**
```bash
cd /home/alonso/Documents/apo/android-app
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

**Option B: Using Gradle (signed - for production)**

First, create a keystore if you don't have one:
```bash
keytool -genkey -v -keystore smartsystems-chat.keystore \
  -alias smartsystems -keyalg RSA -keysize 2048 -validity 10000
```

Then configure signing in `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../smartsystems-chat.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your-password"
            keyAlias = "smartsystems"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "your-password"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... rest of config
        }
    }
}
```

Build signed release:
```bash
export KEYSTORE_PASSWORD="your-password"
export KEY_PASSWORD="your-password"
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### 4. Test the APK

Install and test the APK on a device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Or for release:
adb install -r app/build/outputs/apk/release/app-release.apk
```

Test checklist:
- [ ] App launches successfully
- [ ] WebView loads chat.smartsystems.work
- [ ] Offline mode works (disable network)
- [ ] Online reconnection works
- [ ] Back button navigation works
- [ ] App update check works (will check in 6 hours)

### 5. Commit and Tag

```bash
git add app/build.gradle.kts README.md
git commit -m "Bump version to 1.0.1

- Feature/fix description
- Another change

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
"

git tag -a v1.0.1 -m "Version 1.0.1"
git push origin main --tags
```

### 6. Create GitHub Release

**Option A: Using GitHub CLI (Recommended)**
```bash
gh release create v1.0.1 \
  app/build/outputs/apk/release/app-release.apk \
  --title "SmartSystems Chat v1.0.1" \
  --notes "## What's New

- Bug fixes and performance improvements
- Feature X added
- Fixed issue Y

**Download and install the APK below to update your app.**

The app will automatically check for this update and prompt you to install it."
```

**Option B: Using GitHub Web Interface**
1. Go to https://github.com/alonsobasauri/smartsystems-chat-android/releases/new
2. Choose tag: `v1.0.1`
3. Set release title: `SmartSystems Chat v1.0.1`
4. Add release notes
5. Upload `app-release.apk` as an asset
6. Click "Publish release"

### 7. Verify Auto-Update System

The auto-update system will:
1. Check the GitHub API every 6 hours
2. Compare version codes
3. Show update dialog if newer version found
4. Download APK when user clicks "Descargar"
5. Prompt for installation

To test immediately:
1. Install the old version (v1.0.0)
2. Clear app data to reset update check timer
3. Open the app
4. Check logcat for update check:
```bash
adb logcat | grep -i "update\|version"
```

## Version Naming Convention

### Version Code
Integer that must increase with each release:
- v1.0.0 → versionCode = 10000
- v1.0.1 → versionCode = 10001
- v1.1.0 → versionCode = 10100
- v2.0.0 → versionCode = 20000

Formula: `MAJOR * 10000 + MINOR * 100 + PATCH`

### Version Name
Semantic versioning string:
- **MAJOR**: Breaking changes
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes

## Troubleshooting

### APK Build Fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

### Signing Fails
- Check keystore path is correct
- Verify passwords are correct
- Ensure alias matches the one in keystore

### Update Check Not Working
- Verify GitHub release has APK attached
- Check version tag matches format `v1.0.0`
- Ensure app has internet permission
- Check API rate limits (60 requests/hour unauthenticated)

### Users Not Getting Update
- Verify release is published (not draft)
- Check that APK asset name ends with `.apk`
- Ensure version code is higher than installed version
- User must wait 6 hours between checks (or clear app data)

## Security Notes

### Keystore
- **NEVER** commit keystore to git
- **NEVER** commit passwords to git
- Store keystore securely (backup to encrypted storage)
- Use environment variables for passwords
- Consider using GitHub Secrets for CI/CD

### APK Distribution
- Always sign release APKs
- Test on multiple devices before releasing
- Consider using Google Play Console for wider distribution
- For sideloading, users must enable "Unknown Sources"

## Automation (Optional)

Create `.github/workflows/release.yml` for automated builds:

```yaml
name: Build and Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build APK
        run: |
          chmod +x gradlew
          ./gradlew assembleRelease
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/app-release.apk
```

## Quick Reference

```bash
# Full release process (debug version)
cd /home/alonso/Documents/apo/android-app

# 1. Edit version
vim app/build.gradle.kts  # Increment versionCode and versionName

# 2. Build
./gradlew assembleDebug

# 3. Test
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Commit and tag
git add app/build.gradle.kts README.md
git commit -m "Bump version to X.Y.Z"
git tag -a vX.Y.Z -m "Version X.Y.Z"
git push origin main --tags

# 5. Create release
gh release create vX.Y.Z \
  app/build/outputs/apk/debug/app-debug.apk \
  --title "SmartSystems Chat vX.Y.Z" \
  --notes "Release notes here"
```
