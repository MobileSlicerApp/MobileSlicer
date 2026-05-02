# Local Dependencies

This file records external machine-level resources used during development and where they are exposed inside the project tree.

## Project-local paths

* `.android-sdk` -> project-local Android SDK copy used for builds
* `tools/adb` -> project-local adb entrypoint backed by `.android-sdk/platform-tools/adb`
* `android-app/gradlew` -> local Gradle wrapper script
* `android-app/gradle/wrapper/gradle-wrapper.jar` -> local Gradle wrapper jar

## Notes

* `android-app/local.properties` points to the project-local `.android-sdk` path
* Future coders should use the in-project `.android-sdk` copy rather than any external or Trash-backed SDK path
* Android device and Waydroid validation should prefer `tools/adb`
* Waydroid remains a machine-level dependency installed outside the repository
* On this development machine, Waydroid is the local Android runtime validation environment and runs the project in `x86_64`
* This local `x86_64` Waydroid target is for development/runtime proof; production Android release targets remain ARM, primarily `arm64-v8a`
* Orca vendor code remains under `vendor/orcaslicer/`
