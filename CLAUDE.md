# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Comprarei is an Android shopping-list app (Kotlin, package `br.com.zamfir.comprarei`). Users create "carts" (any kind of purchase: groceries, building materials, etc.), add products with name, optional brand, price, and quantity, see totals, and share a cart as text. Login uses Firebase (email/password or Google), and data is backed up to Firestore. Code comments are in Portuguese. The default `values/strings.xml` is in English and `values-pt/strings.xml` holds the Portuguese translation, so every new string goes in both files.

## Build & test

Gradle 8.14.6 wrapper, AGP 8.13.2, Kotlin 2.4 (Compose compiler plugin), KSP for Room (no kapt), Groovy DSL. Every dependency and plugin version lives in the version catalog `gradle/libs.versions.toml`; never hardcode a version in `build.gradle`. `compileSdk 36`, `targetSdk 34`, `minSdk 27`. The build needs JDK 17 (local install: `~/.jdks/ms-17.0.20.1`).

Version ceilings while the project stays on AGP 8 / compileSdk 36. Newer versions require AGP 9.1+ and compileSdk 37, so do not bump them without migrating AGP first:
- lifecycle 2.10.x
- core-ktx 1.18.x
- Compose BOM 2026.06.x (Compose 1.11)
- Navigation 3 1.2.x, with `lifecycle-viewmodel-navigation3` on the lifecycle version (newer lines pull lifecycle 2.11 / Compose 1.12)
- Coil 3.5.x (3.6 pulls Compose 1.12)

Images: Coil 3 (`ComprareiAvatar` for profile pictures). Drag-to-reorder lists use `sh.calvin.reorderable`. Firebase BOM 34+ has no KTX modules: import `com.google.firebase.Firebase`, `com.google.firebase.auth.auth`, and so on.

```bash
export JAVA_HOME=~/.jdks/ms-17.0.20.1
./gradlew assembleDebug
./gradlew testDebugUnitTest                                   # JVM unit tests
./gradlew testDebugUnitTest --tests "br.com.zamfir.comprarei.core.data.firestore.SyncPlanTest"
./gradlew connectedDebugAndroidTest                           # instrumented; see warning below
./gradlew lint
```

Unit tests use JUnit 5 + AssertK (`android-testing` skill) and live in `app/src/test/java/br/com/zamfir/comprarei/`, mirroring the main package. Keep testable logic in pure functions (for example `planSync`, `applyFilter`, `applySortAndSearch`). The only instrumented test is the template `ExampleInstrumentedTest`.

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
- **`core/database`**: Room `AppDatabase` (version 1, `fallbackToDestructiveMigration()`, schema exported to `app/schemas/`), `dao/` (plus query result classes such as `CategoryUsage` and `CartProgress`), and `entity/` (`Cart`, `Product`, `Category`, `UserInfo`). `Product.cartId` and `Cart.categoryId` are local integer IDs, and every entity also has a `firestoreUUID` used as the Firestore document ID. Entities double as UI models; there are no separate domain models yet. They are mutable, and `id`/`category` live outside the constructor, so copy them with helpers that keep those fields (`Product.copyWith`, `Cart.copyWith`).
- **`core/data`**: repositories shared by several features. Each is a thin wrapper that runs on the injected IO dispatcher (`withContext(dispatcher)`).
  - `firestore/FirestoreRepository` + `FirebaseMapper` handle cloud backup and restore. `backup()` (suspend, awaits every batch, throws on failure) makes the cloud equal to the local DB using the pure `planSync` (`SyncPlan.kt`), so it deletes remote documents whose `firestoreUUID` is missing locally. An empty or partial local DB would therefore wipe cloud data. To prevent this, backup only runs after `ConfigRepository.isRestoreCompleted(uid)` and re-checks it before every batch.
    - After login, `restoreUserData(onStep)` (suspend, throws on failure) downloads everything, replaces the local tables in one transaction, and only then marks the restore as completed. Never let the user into the app after a failed restore.
    - At startup, `MainViewModel` uses `needsRestore()` to send a user whose restore was interrupted back to `SignInRoute(resumeRestore = true)`.
    - A new account is marked ready (`markNewAccountReady`). Logoff and account deletion clear the flag (`clearSession`) before wiping the DB. Logoff from Settings first tries an awaited `backup()`.
  - `preferences/ConfigRepository` (SharedPreferences, file name from `R.string.shared_file_name`): list options, backup, `ThemeMode` (observable with `observeThemeMode()`), and whether notification permission was already asked.
  - `user/UserRepository`, `auth/GoogleAuthClient` (Google sign-in through Credential Manager), `image/ImageCompressor`, and `product/ProductRepository` (after every product change it also rewrites the cart's legacy `total` text, kept only for Firestore compatibility with old app versions).
- **`data/workers/BackupWorker`**: deliberately left in its old package. WorkManager persists the worker class name, and the periodic job (`Constants.PERIODIC_BACKUP_WORK_NAME`) uses `ExistingPeriodicWorkPolicy.KEEP`, so moving the class would break the job already scheduled on users' devices. It injects repositories through `KoinJavaComponent.inject`. `FirestoreRepository` also enqueues it as one-time work for manual backups (`Constants.MANUAL_BACKUP_KEY`).
- **`core/domain`**: `exceptions/` (custom exceptions) and `validation/PasswordRules` (password rules shared by sign-up and profile).
- **`core/designsystem`**: the Compose design system. Reuse it instead of raw Material components; add new shared components here.
  - `theme/`: `ComprareiTheme` (follows the system by default; `MainActivity` passes the user's choice), an M3 brand palette generated from `#186F35` (no dynamic color), typography on the Inter variable font (`res/font/inter_variable.ttf`), and `ColorScheme` extensions: `warning*` (yellow warning role) and `brandBar`/`onBrandBar` (the `#186F35` green with white content in both themes, used by every top bar and the Home header).
  - `components/`: `ComprareiTextField`, `ComprareiButton` (Primary/Secondary/Text/Destructive, with `isLoading`), `ComprareiFab` (solid brand green), `ComprareiTopAppBarDefaults.brandColors()`, `ComprareiConfirmDialog`, `ComprareiErrorDialog` (OK only, readable message, never a stack trace), `ComprareiColorDot` (category color), `ComprareiAvatar` (Coil, round profile picture with themed placeholder), `ComprareiGoogleButton`, `ComprareiWaveBackground` (the login waves), `ComprareiEmptyState`, `ComprareiLoading`/`ComprareiLoadingOverlay`, and `ComprareiPasswordRules`.
- **`core/presentation`**: `UiText` (text from a string resource or a ready string, resolved in the UI), `ObserveAsEvents`, `CurrencyVisualTransformation` (money input, stores cents), `Share`, and `StatusBarIconsEffect` (dark status bar icons on screens without the green bar, such as the login).
- **`core/util`**: `Constants`, `DateUtil`, `Money.kt` (`toCents`, `formatCents`, `formatMoney`), `Network.kt` (`isConectadoInternet`), and `log/AppLogger`. Use `AppLogger.error/warning/debug` for logging: errors go to Crashlytics as non-fatals, and warnings and debug messages become Crashlytics breadcrumbs (plus Logcat in debug). Never use `Log.*` or `printStackTrace()` directly; the only exception is `CrashHandler`, which runs while the process is dying.
- **`feature/<name>/{data,presentation}`**: `auth`, `cart` (the Home), `product`, `category`, `settings`, and `profile` (includes delete account).
  - Each screen follows the `android-presentation-mvi` skill: a ViewModel with `State` (StateFlow), `Action`, and `Event` (Channel), a `<Screen>Root` composable that collects state and events, and a stateless `<Screen>Screen`. Forms are bottom sheets with their own ViewModel (`form/`), hosted by the screen's Root.
  - Each feature's `presentation` has a `<Feature>Navigation.kt` with its `@Serializable` routes (`NavKey`) and an `EntryProviderScope<NavKey>.<feature>Entries(...)` section. Intra-feature navigation uses the back stack; cross-feature navigation goes through lambda callbacks wired in `AppNavigation`. Routes carry IDs and small values only (for example `ProductsRoute(cartId, cartName)`), passed to the ViewModel with `koinViewModel { parametersOf(...) }`.
  - `cart` and `category` have their own `data` repository. Cart filtering and sorting: immutable `CartFilter` and the pure `List<Cart>.applyFilter()` in `feature/cart/presentation/filter/CartFilter.kt`. Product sorting: the pure `List<Product>.applySortAndSearch()` in `feature/product/presentation/ProductSort.kt`.
  - The Home reloads in `LifecycleResumeEffect` when it comes back on screen, so other screens don't need to notify it.
- **`feature/crash`**: `CrashHandler` and `CrashActivity` (Compose, `CrashScreen.kt`; no ViewModel, data comes from Intent extras `CrashActivity.EXTRA_*`; `exported=false`). `CrashHandler.install()` wraps the previous default handler (Crashlytics): it opens `CrashActivity`, then delegates to that handler so the crash is still reported.

The window theme (`Theme.Comprarei` in `values/` and `values-night/themes.xml`) is a plain platform theme that only sets the window background; there is no AppCompat or Material Components dependency. The app draws edge to edge: screens rely on `Scaffold`/`TopAppBar` insets, and fixed bottom bars need `navigationBarsPadding()`.

Money: product prices are `Double` in the DB. Cart totals are never read from the stored `Cart.total` text. They are computed from the products (`ProductDao.progressByCart` → `CartProgress.totalValue`). Compare and filter in cents (`Double.toCents()`) and format only for display with `formatMoney`/`formatCents`, using the `R.string.moeda_real` symbol and the device locale's separators.
