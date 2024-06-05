#!/bin/bash

# Extract the app names
apps=$(jq -r '.apps | join(",")' apps-config.json)

for app in $(echo $apps | tr "," "\n"); do
  echo "./apps/${app}/build/outputs/apk/debug/${app}-debug.apk"
  echo "./apps/${app}/build/outputs/apk/release/${app}-release-unsigned.apk"
done
