#!/bin/bash

# Read the JSON file
json=$(cat your_file.json)

# Extract the app names
apps=$(echo "$json" | jq -r '.apps | join(",")')

# Create the debug and release lines
debug_line="apps/{$apps}/build/outputs/apk/debug/{$apps}-debug.apk"
release_line="apps/{$apps}/build/outputs/apk/release/{$apps}-release-unsigned.apk"

# Print the lines
echo -e "$debug_line\n$release_line"
