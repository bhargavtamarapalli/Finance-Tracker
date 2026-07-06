#!/bin/bash
# Local test runner automation script
set -e

# Define directories
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=========================================="
echo "      Finance Manager Test Automation     "
echo "=========================================="
echo ""

# 1. Run local unit tests (Robolectric)
echo "------------------------------------------"
echo " [1/3] Running local JVM unit tests... "
echo "------------------------------------------"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew clean :app:testDebugUnitTest --no-configuration-cache

echo ""
echo "------------------------------------------"
echo " [2/3] Checking connected devices... "
echo "------------------------------------------"
# Check if ADB path is accessible
ADB_PATH="/Users/bhargavtamarapalli/Library/Android/sdk/platform-tools/adb"
DEVICES=""

if [ -f "$ADB_PATH" ]; then
    DEVICES=$("$ADB_PATH" devices | grep -v "List of devices attached" | grep "device" || true)
fi

if [ -n "$DEVICES" ]; then
    echo "Found connected device(s):"
    echo "$DEVICES"
    echo ""
    echo "------------------------------------------"
    echo " [3/3] Running on-device instrumented tests... "
    echo "------------------------------------------"
    JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:connectedAndroidTest --no-configuration-cache
else
    echo "No connected Android devices found. Skipping instrumented tests."
    echo "Connect a device/emulator and run: ./run_tests.sh --all to force."
fi

echo ""
echo "=========================================="
echo "    All test suites finished successfully! "
echo "=========================================="
