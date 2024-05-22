#!/bin/bash

show_help() {
    echo "Usage: $0 <app>"
    echo "Builds, installs, and starts the specified Android app."
    echo "  <app>       The name of the Android app (e.g., backAndForth)."
    echo "  --help      Display this help message."
}

if [ "$1" == "--help" ]; then
    show_help
    exit 0
fi

# Check if $app is provided as the first argument
if [ -z "$1" ]; then
  echo "Usage: $0 <app>"
  exit 1
fi

app=$1
app_lc=$(echo "$app" | tr '[:upper:]' '[:lower:]')

# Build the app
echo "Building $app..."
./gradlew ":${app}:build"

# Uninstall the app
echo "Uninstalling jp.oist.abcvlib.${app_lc}..."
adb uninstall "jp.oist.abcvlib.${app_lc}"

# Install the app
echo "Installing $app..."
adb install -r "apps/${app}/build/outputs/apk/debug/${app}-debug.apk"

# Start the main activity of the app
echo "Starting MainActivity of jp.oist.abcvlib.${app_lc}..."
adb shell am start -n "jp.oist.abcvlib.${app_lc}/.MainActivity"

echo "Done."
