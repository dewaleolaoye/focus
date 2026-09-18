#!/usr/bin/env bash
set -e

# 1. Resolve Android SDK path
if [ -z "$ANDROID_HOME" ]; then
    if [ -f "local.properties" ]; then
        ANDROID_HOME=$(grep -E '^sdk.dir=' local.properties | cut -d'=' -f2)
    fi
fi
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"

# 2. Resolve Java 17 for Gradle
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    elif /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
    fi
fi

# 3. Check for running devices/emulators
echo "Checking for running Android devices or emulators..."
RUNNING_DEVICES=$("$ADB" devices 2>/dev/null | grep -v "List of devices" | grep "device$" | awk '{print $1}' || true)

if [ -z "$RUNNING_DEVICES" ]; then
    echo "No running device/emulator detected."
    AVD_LIST=$("$EMULATOR" -list-avds 2>/dev/null || true)
    FIRST_AVD=$(echo "$AVD_LIST" | head -n 1)

    if [ -z "$FIRST_AVD" ]; then
        echo "Error: No Android Virtual Devices (AVDs) found."
        echo "Please create an emulator in Android Studio (Tools -> Device Manager) first."
        exit 1
    fi

    echo "Launching emulator: $FIRST_AVD..."
    "$EMULATOR" -avd "$FIRST_AVD" > /dev/null 2>&1 &

    echo "Waiting for emulator to connect..."
    "$ADB" wait-for-device
    echo "Waiting for Android to finish booting..."
    while [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        sleep 2
    done
    echo "Emulator is ready!"
fi

# 4. Build and install app
echo "Building and installing app (sideload debug)..."
./gradlew :app:installSideloadDebug

# 5. Launch the app on device
echo "Launching app..."
"$ADB" shell am start -n com.websiteblocker.app/.MainActivity

echo "Done! App is now running on your device/emulator."
