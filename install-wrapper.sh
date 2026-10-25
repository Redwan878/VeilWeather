#!/bin/bash
# Gradle wrapper installation script

cd "$(dirname "$0")"

if [ ! -f gradlew ]; then
    echo "Installing Gradle wrapper..."
    gradle wrapper --gradle-version=8.2
fi

chmod +x gradlew
./gradlew --version