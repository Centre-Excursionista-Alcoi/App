#!/usr/bin/env sh

./gradlew --no-daemon :composeApp:packageDistributionForCurrentOS || cat /source/composeApp/build/compose/logs/createRuntimeImage/*-err.txt

cp /source/composeApp/build/compose/binaries/main/* /output
