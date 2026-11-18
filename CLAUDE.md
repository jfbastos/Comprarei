# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Comprarei is an Android shopping-list app (Kotlin, package `br.com.zamfir.comprarei`). Users create "carts" (any kind of purchase: groceries, building materials, etc.), add products with name, optional brand, price, and quantity, see totals, and share a cart as text. Login uses Firebase (email/password or Google), and data is backed up to Firestore. Code comments are in Portuguese. The default `values/strings.xml` is in English and `values-pt/strings.xml` holds the Portuguese translation, so every new string goes in both files.

## Build & test

Gradle 9.9.1 wrapper, AGP 9.5.1, Kotlin 2.4 (Compose compiler plugin), KSP for Room (no kapt), Groovy DSL (use `=` assignments; the space syntax is deprecated). Every dependency and plugin version lives in the version catalog `gradle/libs.versions.toml`; never hardcode a version in `build.gradle`. `compileSdk 37`, `targetSdk 36`, `minSdk 27`. The build needs JDK 17 (local install: `~/.jdks/ms-17.0.20.1`).

AGP 9 has built-in Kotlin: do not apply `org.jetbrains.kotlin.android`, and do not add opt-outs such as `android.builtInKotlin=false` or `android.newDsl=false`. The Kotlin jvmTarget follows `compileOptions.targetCompatibility`. AGP 9 would pull KGP 2.2.10, so the root `build.gradle` pins KGP to the catalog's `kotlin` version through `buildscript { classpath libs.kotlin.gradle.plugin }`; when bumping Kotlin, keep KGP, the Compose compiler plugin and serialization on the same version. Raising `compileSdk` past 37 requires a newer AGP (9.5 supports up to API 37).

Images: Coil 3 (`ComprareiAvatar` for profile pictures). Drag-to-reorder lists use `sh.calvin.reorderable`. Firebase BOM 34+ has no KTX modules: import `com.google.firebase.Firebase`, `com.google.firebase.auth.auth`, and so on.

```bash
export JAVA_HOME=~/.jdks/ms-17.0.20.1
./gradlew assembleDebug
./gradlew testDebugUnitTest                                   # JVM unit tests
./gradlew testDebugUnitTest --tests "br.com.zamfir.comprarei.core.data.firestore.SyncPlanTest"
./gradlew connectedDebugAndroidTest                           # instrumented; see warning below
./gradlew lint
```

Unit tests use JUnit 5 + AssertK (`android-testing` skill) and live in `app/src/test/java/br/com/zamfir/comprarei/`, mirroring the main package. Keep testable logic in pure functions (for example `planSync`, `applyFilter`, `applySortAndSearch`). Instrumented tests: `core/database/migration/MigrationTest` (Room `MigrationTestHelper`, uses its own `migration-test` DB) and the template `ExampleInstrumentedTest`. To run one on the phone: build `assembleDebug assembleDebugAndroidTest`, `adb install -r` both APKs, then `adb shell am instrument -w -e class <test class> br.com.zamfir.comprarei.test/androidx.test.runner.AndroidJUnitRunner`.

**Device safety:** the debug build uses the same `applicationId` as the production app, so installing it over a production install shares (and can destroy) its local data. `connectedAndroidTest` uninstalls the app when it finishes, which wipes local data. Run it only on an emulator. On a physical device, use `adb install -r` with `adb shell am instrument -w ...` instead.

`app/google-services.json` is required for the build (Firebase/Crashlytics plugins).

## Modules

Single module: `:app`. (A former `:app:telegram` logging module was removed; its old bot token is still in git history, never reuse or print it.)

## Architecture (MVI, Compose, single Activity, package-by-feature)

Code under `app/src/main/java/br/com/zamfir/comprarei/` is organized by feature first, then by layer. It follows the `android-module-structure` skill, but as packages inside the single `:app` module, so the dependency rules are a convention and the compiler does not enforce them:
- Features never import from each other. Code used by two or more features goes to `core/`.
- `core` never imports from `feature`. Only `app/` imports from several features (to wire navigation and DI).

Resources (`res/`) are not split by feature. There are no layouts, menus, or nav graphs left: every screen is Compose.

- **`app/`**:
  - `MyApplication` starts Koin and installs `CrashHandler`.
  - `MainActivity` is the only Activity besides `CrashActivity`. It is a `ComponentActivity` with the splash screen (kept until `MainViewModel` knows whether a user is logged in), edge-to-edge, the theme from `ThemeMode`, the one-time notification permission request, and the periodic backup scheduling.
  - `navigation/AppNavigation.kt` has the single Navigation 3 `NavDisplay` (follow the `android-navigation` skill). The back stack starts at `SignInRoute` or `CartsRoute`; logging in, logging out, and deleting the account replace the whole back stack, so back never crosses that boundary.
  - `di/AppModules.kt`: `appModules` aggregates every Koin module passed to `startKoin`. Each layer declares its own module next to its code:
    - `core/database/di/coreDatabaseModule`: the DB and the DAOs
    - `core/data/di/coreDataModule`: the IO dispatcher `named(Constants.IO_DISPATCHER)` and the shared repositories
    - `feature/<name>/data/<Name>DataModule.kt` and `feature/<name>/presentation/<Name>PresentationModule.kt` (ViewModels via `viewModelOf`, or `viewModel { params -> }` when the route passes arguments)

  A new repository or ViewModel goes in its layer's module, and a new module must be added to `appModules`.
- **`core/database`**: Room `AppDatabase` (version 1, schema exported to `app/schemas/`), `dao/` (plus query result classes such as `CategoryUsage` and `CartProgress`), and `entity/` (`Cart`, `Product`, `Category`, `UserInfo`). `Product.cartId` and `Cart.categoryId` are local integer IDs, and every entity also has a `firestoreUUID` used as the Firestore document ID. Entities double as UI models; there are no separate domain models yet. They are immutable data classes (change them with `copy()`), and `id` is the last constructor parameter on purpose: the parameter order is the column order in the schema, and changing it changes Room's identity hash, so the production database would no longer open. Writes to Firestore serialize the entity by reflection, so property names are also the Firestore field names. A cart's category is not part of `Cart`: screens look it up by `categoryId` (for example `CartsState.categories`).
  - No destructive migration: a lost local DB would make the next backup wipe the user's cloud data, so a missing migration must crash instead. To change the schema, bump `version`, build, commit the generated `app/schemas/.../N.json`, and add a single-step `Migration(N, N + 1)` to `migration/DatabaseMigrations.ALL` (steps in its KDoc). `DatabaseMigrationsTest` (JVM) fails if a schema version has no migration, and `MigrationTest` (instrumented) validates the SQL against the exported schemas.
- **`core/data`**: repositories shared by several features. Each is a thin wrapper that runs on the injected IO dispatcher (`withContext(dispatcher)`).
  - `firestore/FirestoreRepository` + `FirebaseMapper` handle cloud backup and restore. `backup()` (suspend, awaits every batch, throws on failure) makes the cloud equal to the local DB using the pure `planSync` (`SyncPlan.kt`), so it deletes remote documents whose `firestoreUUID` is missing locally. An empty or partial local DB would therefore wipe cloud data. To prevent this, backup only runs after `ConfigRepository.isRestoreCompleted(uid)` and re-checks it before every batch.
    - After login, `restoreUserData(onStep)` (suspend, throws on failure) downloads everything, replaces the local tables in one transaction, and only then marks the restore as completed. Never let the user into the app after a failed restore.
    - At startup, `MainViewModel` uses `needsRestore()` to send a user whose restore was interrupted back to `SignInRoute(resumeRestore = true)`.
    - A new account is marked ready (`markNewAccountReady`). Logoff and account deletion clear the flag (`clearSession`) before wiping the DB. Logoff from Settings first tries an awaited `backup()`.
  - The Firebase project is on the free Spark plan, which has no Cloud Storage access (uploads fail with HTTP 402). Never use Firebase Storage. The profile picture is a JPEG (≤512px, from `ImageCompressor`) stored as a `Blob` in Firestore at `{uid}/user_data/Profile/picture` (`FirestoreRepository.uploadProfilePicture`/`downloadProfilePicture`, deleted by `deleteUserData`). The local copy lives in `filesDir/profile_picture/` (never the cache), and `UserRepository.ensureLocalProfilePicture()` downloads it again when it is missing.
  - `preferences/ConfigRepository` (SharedPreferences, file name from `R.string.shared_file_name`): list options, backup, `ThemeMode` (observable with `observeThemeMode()`), and whether notification permission was already asked.
  - `user/UserRepository`, `auth/GoogleAuthClient` (Google sign-in through Credential Manager), `image/ImageCompressor`, and `product/ProductRepository` (after every product change it also rewrites the cart's legacy `total` text, kept only for Firestore compatibility with old app versions).
- **`data/workers/BackupWorker`**: deliberately left in its old package. WorkManager persists the worker class name, and the periodic job (`Constants.PERIODIC_BACKUP_WORK_NAME`) uses `ExistingPeriodicWorkPolicy.KEEP`, so moving the class would break the job already scheduled on users' devices. It injects repositories through `KoinJavaComponent.inject`. `FirestoreRepository` also enqueues it as one-time work for manual backups (`Constants.MANUAL_BACKUP_KEY`).
- **`core/domain`**: `exceptions/` (custom exceptions) and `validation/PasswordRules` (password rules shared by sign-up and profile).
- **`core/designsystem`**: the Compose design system. Reuse it instead of raw Material components; add new shared components here.
  - `theme/`: `ComprareiTheme` (follows the system by default; `MainActivity` passes the user's choice), an M3 brand palette generated from `#186F35` (no dynamic color), typography on the Inter variable font (`res/font/inter_variable.ttf`), and `ColorScheme` extensions: `warning*` (yellow warning role) and `brandBar`/`onBrandBar` (the `#186F35` green with white content in both themes, used by every top bar and the Home header).
  - `components/`: `ComprareiTextField`, `ComprareiButton` (Primary/Secondary/Text/Destructive, with `isLoading`), `ComprareiFab` (solid brand green), `ComprareiTopAppBarDefaults.brandColors()`, `ComprareiConfirmDialog`, `ComprareiErrorDialog` (OK only, readable message, never a stack trace), `ComprareiColorDot` (category color), `ComprareiAvatar` (Coil, round profile picture with themed placeholder), `ComprareiGoogleButton`, `ComprareiWaveBackground` (the login waves), `ComprareiEmptyState`, `ComprareiLoading`/`ComprareiLoadingOverlay`, `ComprareiPasswordRules`, and `rememberUnsavedSwipeToDismissBoxState` (use it instead of Material's `rememberSwipeToDismissBoxState` in lists with swipe-to-delete and undo: the Material state is saveable, so an undone item comes back "dismissed" and gets deleted again).
- **`core/presentation`**: `UiText` (text from a string resource or a ready string, resolved in the UI), `ObserveAsEvents`, `CurrencyVisualTransformation` (money input, stores cents), `Share`, and `StatusBarIconsEffect` (dark status bar icons on screens without the green bar, such as the login).
- **`core/util`**: `Constants`, `DateUtil`, `Money.kt` (`toCents`, `formatCents`, `formatMoney`), and `log/AppLogger`. Use `AppLogger.error/warning/debug` for logging: errors go to Crashlytics as non-fatals, and warnings and debug messages become Crashlytics breadcrumbs (plus Logcat in debug). Never use `Log.*` or `printStackTrace()` directly; the only exception is `CrashHandler`, which runs while the process is dying.
- **`feature/<name>/{data,presentation}`**: `auth`, `cart` (the Home), `product`, `category`, `settings`, and `profile` (includes delete account).
  - Each screen follows the `android-presentation-mvi` skill: a ViewModel with `State` (StateFlow), `Action`, and `Event` (Channel), a `<Screen>Root` composable that collects state and events, and a stateless `<Screen>Screen`. Forms are bottom sheets with their own ViewModel (`form/`), hosted by the screen's Root.
  - Each feature's `presentation` has a `<Feature>Navigation.kt` with its `@Serializable` routes (`NavKey`) and an `EntryProviderScope<NavKey>.<feature>Entries(...)` section. Intra-feature navigation uses the back stack; cross-feature navigation goes through lambda callbacks wired in `AppNavigation`. Routes carry IDs and small values only (for example `ProductsRoute(cartId, cartName)`), passed to the ViewModel with `koinViewModel { parametersOf(...) }`.
  - `cart` and `category` have their own `data` repository. Cart filtering and sorting: immutable `CartFilter` and the pure `List<Cart>.applyFilter()` in `feature/cart/presentation/filter/CartFilter.kt`. Product sorting: the pure `List<Product>.applySortAndSearch()` in `feature/product/presentation/ProductSort.kt`.
  - The Home (`CartsViewModel`) and Products (`ProductsViewModel`) observe Room `Flow`s (and `ConfigRepository.observe*` for preferences) and never reload by hand: after a write, Room emits the new data, so other screens don't need to notify them. Optimistic changes (toggle done, delete, drag) are applied to the state at once and reverted to the last list from the database if the write fails. While a drag is in progress, database emissions don't replace the list. Read functions that run inside `runInTransaction` (backup, restore, cart deletion) stay blocking, because suspend DAO calls are not allowed there.
- **`feature/crash`**: `CrashHandler` and `CrashActivity` (Compose, `CrashScreen.kt`; no ViewModel, data comes from Intent extras `CrashActivity.EXTRA_*`; `exported=false`). `CrashHandler.install()` wraps the previous default handler (Crashlytics): it opens `CrashActivity`, then delegates to that handler so the crash is still reported.

The window theme (`Theme.Comprarei` in `values/` and `values-night/themes.xml`) is a plain platform theme that only sets the window background; there is no AppCompat or Material Components dependency. The app draws edge to edge: screens rely on `Scaffold`/`TopAppBar` insets, and fixed bottom bars need `navigationBarsPadding()`.

Money: product prices are `Double` in the DB. Cart totals are never read from the stored `Cart.total` text. They are computed from the products (`ProductDao.observeProgressByCart` → `CartProgress.totalValue`). Compare and filter in cents (`Double.toCents()`) and format only for display with `formatMoney`/`formatCents`, using the `R.string.moeda_real` symbol and the device locale's separators.

## Refactor in progress

The app is going through a staged refactor. Stages 1-5 are done and committed (Telegram removal, package-by-feature, build modernization, Compose migration of every screen, backup/money/migration bug fixes, AGP 9); git history has the details. Stage 6 is an architecture review, done one sub-stage at a time, in order:

- 6.1 Profile picture out of the cache and stored in Firestore: done (`e103032`).
- 6.2 Immutable entities with `id` in the constructor, no `Serializable`: done (`7980e6b`).
- 6.3 Suspend/`Flow` DAOs and reactive ViewModels (Products, Home, Categories): done (`7980e6b`). `CartForm` and `CartFilter` keep one-shot reads on purpose.
- **6.4 (next)**: repository interfaces, plus a session/account orchestrator that takes login, logoff, restore and account deletion out of the ViewModels, with ViewModel tests (JUnit 5, Turbine, fake repositories).
- 6.5 Typed errors with `Result<T, E>` (`android-error-handling` skill) instead of exceptions carrying localized text.
- 6.6 Shared selection/undo helpers, and split the big screens (`CartsScreen`, `ProductsScreen`).
- 6.7 Domain models and Firestore DTOs (optional).

Known open points: product search is not accent-insensitive ("cafe" does not find "Café"; offered as an optional improvement). Firestore security rules live only in the Firebase console, not in the repo. The profile picture flow and the backup notification were not verified on the test phone (its account has no photo, and `POST_NOTIFICATIONS` is denied there). Google sign-in on a debug build fails with "[16] Account reauth failed" until the debug keystore SHA-1 is registered in the Firebase console (a configuration issue, not code).

### How to work on the refactor

- One sub-stage at a time; stop for the user's review after each one. For UI changes, propose options and wait for the user's choice before implementing.
- Never commit without an explicit request from the user.

### Testing on devices

- The user authorized the physical phone motorola edge 30 neo (`adb -s ZF5247H754`). Other zamfir apps are installed on it: never touch other packages. Use `adb install -r`; never uninstall and never run `connectedAndroidTest` there. Fallback emulator: `Medium_Phone_API_36` (no account, good for checking login screens). Don't restart the emulator unless asked.
- Claude can drive the phone with `adb` plus `uiautomator dump` to reproduce bugs. Don't create or delete the user's data; if a test changes it, put it back as it was.
- Logoff wipes the local DB, so check login screens on the emulator instead of logging out the phone. A Google login over an existing session mixes the other account's cloud data into the local DB. Never test a real account deletion on the user's account.
- To test the crash screen: `adb shell am crash br.com.zamfir.comprarei`, and wait about 3 minutes between crashes, or Android stops restarting the app ("crashed quickly").
- Git Bash on Windows: set `MSYS_NO_PATHCONV=1` for adb paths and pass Windows-style `C:/...` paths to `adb install`. Bash heredocs strip backslashes, so edit Android strings that contain apostrophes with the Edit tool.

### Compose gotchas found so far

- Read strings in Compose with `LocalResources`, not `LocalContext` (lint). Portuguese plurals need both `one` and `many`, with `%d`.
- Never put a `DateRangePicker` inside a `DatePickerDialog` (it becomes too narrow); use a full-screen `Dialog` (`usePlatformDefaultWidth = false`) with its own top bar.
