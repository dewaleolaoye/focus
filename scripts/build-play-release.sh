#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
python3 scripts/prepare-play-submission.py
./gradlew :app:testPlayDebugUnitTest :app:lintPlayRelease :app:bundlePlayRelease
python3 -c 'import zipfile; p="app/build/outputs/bundle/playRelease/app-play-release.aab"; z=zipfile.ZipFile(p); assert any(n.startswith("META-INF/") and n.endswith((".RSA", ".EC", ".DSA")) for n in z.namelist()), "Bundle has no upload signature"'
jarsigner -verify app/build/outputs/bundle/playRelease/app-play-release.aab
printf '%s\n' 'Release bundle built. Complete the Play Console declarations and review steps in docs/play/SUBMISSION.md.'
