#!/bin/bash

source android-build.source
./gradlew --no-daemon assembleRelease
adb install ./composeApp/build/outputs/apk/release/composeApp-release.apk
adb shell am start -n org.centrexcursionistalcoi.app/org.centrexcursionistalcoi.app.MainActivity
