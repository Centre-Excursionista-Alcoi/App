# Native SwiftUI UI for iOS — progress and what's left

The iOS app is moving from one Compose view to native SwiftUI, with **all the logic staying in KMP**. This is the
hand-off: what exists, how to work on it, and what is missing. Design rationale and the original plan: the stack of PRs
`ios/01` … `ios/06` and `AGENTS.md` ("Native SwiftUI on iOS").

## Decisions (agreed with the project owner)

- **Phased hybrid.** The SwiftUI shell ships first; screens move over one at a time. A screen without a native version runs
  in a **Compose island** (`composeApp/src/iosMain/.../bridge/ComposeIslands.kt`), so the app always works.
- **Bridge: SKIE** (Flow → `AsyncSequence`, suspend → `async`, sealed → Swift enum via `onEnum(of:)`).
- **Minimum iOS 26** (Liquid Glass APIs used directly). Xcode 27 on the Mac.
- **Kotlin is pinned to 2.4.20**: SKIE 0.10.15 refuses 2.4.21. Don't take a Kotlin bump until SKIE lists it.
- Swift sources live in `iosApp/iosApp/UI/` (the Xcode project picks up new files by itself). A separate Swift package
  can't import the embedded `ComposeApp` framework, so the planned `CEAUI` package was not made.

## Done (stacked PRs, bottom to top)

| Branch | What |
|---|---|
| `ios/01-skie-settings-proof` | SKIE, `ViewModelFactory` (Kotlin → Swift ViewModel access), `FlowState`, first native screen, iOS 26 target |
| `ios/02-native-shell` | `RootView`/`ShellModel` (initialising → loading → login → main → logout), adaptive `TabView`, `MainTab`/`mainTabs()` shared rules, Compose islands, `AppEntries.kt` (post-login Compose screens, reused by islands), String Catalog generator |
| `ios/03-native-auth` | Native sign in + 3-step register on `LoginViewModel`; `AuthError` (Kotlin) for wording errors; `L10n`/`LText` for strings with placeholders |
| `ios/04-native-settings-security` | Native Settings and Security; `authError` moved to `ErrorViewModel`; `RouteView` picks native screen or island |
| `ios/05-native-home` | Native Home (events, posts, assistance sign-up via `EventAssistanceState`, native calendar editor, `ImageLoading`) |
| `ios/06-native-profile` | Native Profile (member card, insurances + add form, FEMECV login, departments); one `NavigationStack` per tab; debug launch flags |

**Verified on the simulator** (iPhone 17, iOS 27, against a local server): sign in, loading, the tab bar (Liquid Glass),
Home with synced data (department-scoped events/posts correctly hidden from a non-member), Profile. **Not exercised**:
registering, passkeys, browser login, Security changes, FEMECV login, adding an insurance, event sign-up, the calendar editor,
Settings/Security after the navigation restructure, iPad / wide layouts, dark mode, Catalan/Spanish.

## Not done yet

Branch `ios/wip-native-lendings` has an **unfinished, non-compiling** start of the Lendings tab (catalog, shopping list,
history; spaces still Compose). First error to fix: `LendingsModel.swift:71` — `KotlinUuid` has no `toString()`; use
`description()` or `toHexDashString()` (used elsewhere). It has never run.

Still Compose islands (in the rough order they should be ported):

1. **Lendings tab** (finish the WIP), **Lending tab** (active lending: `LendingDetailsScreen_Content`, QR for pickup, memory)
2. Item type details, lending sign up, lending creation, lending details (`Destination.*`)
3. **Space lendings**: spaces tab, details, creation (calendar), lending details (payments, keys, report)
4. **Activities** tab: memories list, `ActivityMemoryEditor` (rich text → needs a Markdown editor decision; `RichTextState` and
   `PlatformFile` leak into several ViewModels: replace with plain `String`/paths, as `ProfilePageModel.addInsurance` does)
5. **Management** tab: generic list/detail CRUD for lendings, memories, departments, users, posts, events, inventory,
   spaces; `LendingManagementScreen` (QR scanner → `DataScannerViewController`; NFC is unsupported on iOS today)
6. Dialogs still in Compose: `PasskeyReminderSheet` (shown by the Compose `MainScreen`, **not shown by the native shell**),
   insurance document viewer (nothing opens PDFs on iOS today; use QuickLook), update dialogs (not needed on iOS)
7. Then: retire the Compose fallback on iOS (and optionally drop Compose from the iOS framework).

Also missing / worth doing:

- Deep link `Main(showingAdminItemTypeId)` only selects the Management tab: the selection is not passed on to the page.
- The Management tab badge, the "Admin" badge and the sync button exist; pull-to-refresh does not (islands can't).
- `ShellModel`: push notification routing is not wired for iOS (`PlatformInitializerViewModel` gets `null, null`), as before.
- CI: `release.yml` / `development-release.yml` untouched; nothing runs the Swift side. Add `xcodebuild test` and a step that runs
  `scripts/generate_xcstrings.py` and fails on a diff.
- Strings: `String(localized:)` in a few places ignores the in-app language (only `Text`/`LText` follow `\.locale`).
- Foldable: the layout is size-class based only. Xcode 27 has no foldable simulator; blog-reported fold APIs were never confirmed.
- `Localizable.xcstrings` is generated: re-run `python3 scripts/generate_xcstrings.py` after changing any `strings.xml`.
- Markdown is rendered inline-only (`AttributedString`): lists/headings show as text.
- Remove (or keep) the DEBUG-only `DebugLaunch` flags deliberately.

## Working on it

**Mac** (`arnyminerz@192.168.1.121`, repo at `/Volumes/DevDisk/GitProjects/CEA-App`): build with
`SERVER_URL=http://localhost:8080 xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -configuration Debug -destination "platform=iOS Simulator,name=iPhone 17" -derivedDataPath /tmp/cea-dd build`.
(A plain build bakes in the **production** server.) The framework alone: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`.
Kotlin for iOS only compiles on the Mac; Android/common: `./gradlew :composeApp:compileAndroidMain`; DI graph:
`./gradlew :composeApp:jvmTest --tests "org.centrexcursionistalcoi.app.di.TestKoinModules"`.

**Dev server + data** (this is how the screens were checked):

1. `docker compose -f compose.yml -f compose.dev.yml up -d db` (Docker needs the user in the `docker` group; in an old shell use `sg docker -c "…"`).
2. Run the server with the env vars in `AGENTS.md` §2 (`KEYS_PATH=$PWD/server/keys`, `FILES_PATH=$PWD/server/files`).
3. Seed users: insert into `user_references` with the password stored as `base64(IV + AES-256-CBC(bcrypt hash))` of `DevPassword123`
   using `server/keys/aes.key` (AGENTS.md §2); events/posts/departments can be inserted with plain SQL (see git history of this
   file's author for the statements used: a department, two events — one department-scoped — and a post).
4. The simulator reaches the host through the Mac: `ssh -f -N -R 8080:localhost:8080 arnyminerz@192.168.1.121`.
5. Drive the simulator (no tapping from CLI) with the debug launch arguments of `UI/Shell/DebugLaunch.swift`:
   `-CEASkipNotificationPrompt -CEADebugLogin email:password -CEADebugTab profile -CEADebugOpen settings -CEADebugSync`
   via `xcrun simctl launch <udid> org.centrexcursionistalcoi.app <args>` and `xcrun simctl io <udid> screenshot`.
   A stuck system permission alert can only be cleared with `xcrun simctl erase <udid>`.
6. Stop everything afterwards: the server, the tunnel, `docker compose -f compose.yml -f compose.dev.yml down`.

## Conventions worth keeping

- A Swift `…Model` (`@MainActor @Observable`) wraps one shared ViewModel from `ViewModelFactory`, collects flows with `collect(_:_:)`,
  and calls `ViewModelFactory.shared.dispose(viewModel:)` in `deinit`. No business rules in Swift: if a rule lives in a composable,
  move it to Kotlin first (examples: `MainTab`, `EventAssistanceState`, `AuthError`, `InsuranceRow`, `LendingsPageModel.catalog`).
- Kotlin names in Swift: types from `:shared` are `Shared…` (aliases in `Bridge/SharedTypes.swift`); nested classes are flattened
  (`DestinationLogin`); extension functions are methods on the receiver; `description` properties are `description_`; nullable
  list items arrive as `Any`; `kotlin.time.Instant` is `KotlinInstant` (`.date` in `Bridge/KotlinTypes.swift`).
- Strings: key = Compose resource name; `Text("key")` for plain ones, `LText("key", args…)` for placeholders.
