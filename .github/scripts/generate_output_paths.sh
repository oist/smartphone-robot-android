#!/bin/sh
files=$(jq -r '.apps[] | "./apps/" + . + "/build/outputs/apk/debug/" + . + "-debug.apk ./apps/" + . + "/build/outputs/apk/release/" + . + "-release.apk"' apps-config.json)
echo "files=$files" >> $GITHUB_ENV
