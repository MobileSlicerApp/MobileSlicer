# Contributing

MobileSlicer is in beta-stage development. Bug reports, printer compatibility
notes, build failures, and clear reproduction cases are the most useful
contributions right now.

## Before Opening an Issue

- Check the latest `main` branch and existing issues.
- Include the MobileSlicer version or commit you tested.
- Include your Android device model and Android version.
- For printer issues, include printer model, firmware, connection type, and
  whether export/share still works.
- Remove private model files, API keys, printer passwords, Wi-Fi details, and
  account information from logs or screenshots.

## Local Build Check

```bash
cd android-app
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Release signing files are local-only and are not accepted in pull requests.

## Pull Requests

Small, focused pull requests are easiest to review. Keep app changes, native
engine changes, and generated asset changes separate when possible.

For changes that affect slicing, preview, printer communication, profile import,
or file export, include the test case or manual verification steps used.
