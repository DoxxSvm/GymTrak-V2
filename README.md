This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/backend](./backend) contains the NestJS API used by the mobile app. It lives on the `backend` git branch.

### Branches

- `main` — mobile app (Android/iOS)
- `backend` — mobile app + NestJS API server

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

1. Copy `local.properties.example` to `local.properties` and set `MAPS_API_KEY`.
2. Copy `composeApp/google-services.json.example` to `composeApp/google-services.json` from the Firebase console.

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

1. Copy `iosApp/iosApp/GoogleService-Info.plist.example` to `iosApp/iosApp/GoogleService-Info.plist` from the Firebase console.
2. Run `pod install` in `iosApp/` if needed.

### Build and Run Backend API

On the `backend` branch:

1. Copy `backend/.env.example` to `backend/.env` and set secrets locally.
2. Copy `backend/secrets/firebase-adminsdk.json.example` to `backend/secrets/firebase-adminsdk.json` if using FCM.
3. From `backend/`: `npm install`, `npx prisma generate`, `npm run start:dev`.

Never commit `backend/.env`, Firebase service account JSON, or files under `backend/uploads/`.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…