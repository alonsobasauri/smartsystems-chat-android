# Build Instructions

## Quick Start - Building the APK

Since this machine doesn't have Android SDK installed, you'll need to build the APK on a machine with Android development tools.

### Option 1: Android Studio (Recommended)

1. **Install Android Studio**
   - Download from: https://developer.android.com/studio
   - Install Android SDK 34 during setup

2. **Open Project**
   ```bash
   # Open Android Studio
   # File > Open > Select /home/alonso/Documents/apo/android-app
   ```

3. **Sync Gradle**
   - Android Studio will automatically sync
   - Wait for dependencies to download

4. **Build APK**
   - Build > Build Bundle(s) / APK(s) > Build APK(s)
   - APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Command Line (Requires Android SDK)

1. **Install Android SDK Command Line Tools**
   ```bash
   # Download from: https://developer.android.com/studio#command-tools
   # Extract to ~/Android/Sdk/cmdline-tools/latest

   # Set environment variables
   export ANDROID_HOME=~/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

2. **Install SDK Components**
   ```bash
   sdkmanager "platforms;android-34" "build-tools;34.0.0"
   ```

3. **Build APK**
   ```bash
   cd /home/alonso/Documents/apo/android-app
   ./gradlew assembleDebug
   ```

### Option 3: GitHub Actions (Automated)

The repository can be configured with GitHub Actions to automatically build APKs:

1. Create `.github/workflows/build.yml`:
```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

2. Push changes and download artifact from Actions tab

## Creating Your First Release

Once you have built the APK:

```bash
# 1. Copy APK to releases directory
cp app/build/outputs/apk/debug/app-debug.apk releases/smartsystems-chat-v1.0.0.apk

# 2. Commit the APK
git add releases/smartsystems-chat-v1.0.0.apk
git commit -m "Add v1.0.0 release APK"
git push

# 3. Create GitHub release
gh release create v1.0.0 \
  releases/smartsystems-chat-v1.0.0.apk \
  --title "SmartSystems Chat v1.0.0" \
  --notes "## Initial Release

Features:
- Full-screen WebView for chat.smartsystems.work
- Automatic offline detection
- Custom offline page
- Auto-update system from GitHub releases
- Network monitoring
- Back button navigation

**Download the APK and install it on your Android device (Android 9.0+)**"
```

## Alternative: Transfer to Another Machine

If you have another computer with Android SDK:

```bash
# 1. Clone the repository
git clone https://github.com/alonsobasauri/smartsystems-chat-android.git
cd smartsystems-chat-android

# 2. Build
./gradlew assembleDebug

# 3. Copy APK back to this machine
# Use scp, USB drive, cloud storage, etc.
scp app/build/outputs/apk/debug/app-debug.apk user@this-machine:~/Downloads/
```

## Minimum Requirements for Building

- Java JDK 17 or higher
- Android SDK (Platform 34)
- 4GB RAM minimum (8GB recommended)
- 10GB free disk space

## Troubleshooting

### Missing Java
```bash
# Install OpenJDK 17
sudo apt update
sudo apt install openjdk-17-jdk

# Verify
java -version
```

### Gradle Sync Fails
```bash
# Clean and retry
./gradlew clean
./gradlew assembleDebug
```

### SDK Not Found
Set `ANDROID_HOME` environment variable:
```bash
export ANDROID_HOME=~/Android/Sdk
```

Or create `local.properties`:
```properties
sdk.dir=/home/your-username/Android/Sdk
```
