# AGENTS.md — CEA App developer/agent guide

Practical knowledge for working in this repo: how to spin up a local dev environment, how to drive the
Android app on an emulator from the CLI, how the codebase is organized, and non-obvious gotchas that will
otherwise cost you an hour each. Written after a session that hit most of these the hard way — the "would
have been useful to know beforehand" list is real.

## 1. Project shape

Kotlin Multiplatform monorepo, four Gradle modules (`settings.gradle.kts`):

| Module | What it is |
|---|---|
| `:shared` | Pure data/DTO layer shared by client and server (`data/`, serializers, `DepartmentRole` enum). No platform code. |
| `:composeApp` | The KMP client (Compose Multiplatform): `commonMain` + `androidMain`/`iosMain`/`jvmMain` source sets. This is where almost all client code lives, including Android-specific bits that don't need a full separate app module. |
| `:android` | Thin Android **application** wrapper around `:composeApp` (manifest, launcher activity, debug-only config). Build/install targets go through this module, not `:composeApp` directly. |
| `:server` | Ktor server: routes, Exposed/Postgres persistence, auth, background sync push. |
| `iosApp/` | Xcode project consuming `:composeApp`'s iOS framework. Not a Gradle module. |

Inside `:composeApp/src/commonMain/kotlin/org/centrexcursionistalcoi/app/`:

- `data/` — shared-with-server DTOs actually re-exported from `:shared`, plus client-only view models of data
- `database/` — Room repositories (local persistence), one per entity (`DepartmentsRepository`, `UsersRepository`, ...)
- `network/` — `*RemoteRepository` classes, one per entity, talk to the server over Ktor client
- `sync/` — `BackgroundJob` subclasses + `BackgroundJobCoordinator` (platform expect/actual) driving periodic sync
- `viewmodel/` (+ `viewmodel/management/`) — one ViewModel per screen/feature
- `ui/` — `screen/`, `page/` (incl. `page/main/management/*ListView.kt` — the generic admin CRUD screens), `dialog/`, `reusable/`
- `di/` — Koin wiring, see §3
- `auth/` — `AuthBackend`, session/token handling

Inside `:server/src/main/kotlin/org/centrexcursionistalcoi/app/`:

- `routes/` — one file per entity (`DepartmentRoutes.kt`, `EventsRoutes.kt`, ...), most CRUD goes through the
  generic `provideEntityRoutes` in `routes/RoutesBase.kt`
- `security/` — `DepartmentPermissions.kt` (RBAC checks, see §5), session handling
- `database/` — Exposed tables/entities, `Database.kt` (connection config + **migrations** — see §2 gotchas)
- `notifications/`, `integration/` — email/push/Telegram/FEMECV/CEA external integrations

## 2. Local dev environment

### Server

The **easiest correct path** is what `README.md` already documents — don't reinvent it:

```shell
docker compose -f compose.yml -f compose.dev.yml up -d db
```

This starts just Postgres (with `compose.dev.yml` exposing `5432` to the host — the base `compose.yml` also
defines a `redis` service and the server itself as containers, but for active server development you want
Postgres in Docker and the server running on the host via Gradle so you get fast iteration).

Redis is **optional** — `RedisStoreMap` (used for If-Modified-Since caching) falls back to an in-memory map
if `REDIS_ENDPOINT` isn't set. Don't bother starting the `redis` compose service for local dev.

Required env vars for `:server:run` (recommended dev values, from `README.md`):

```
APP_VERSION=0.0.0
KEYS_PATH=./keys
DB_DRIVER=org.postgresql.Driver
DB_URL=jdbc:postgresql://127.0.0.1:5432/postgres
DB_USER=postgres
DB_PASS=1234567890abcdef
ENV=development
```

Then:

```shell
./gradlew :server:run
```

(`:server:run` is the plain Gradle application task — simpler than `installDist` + running the jar by hand,
and it picks up code changes on restart without reinstalling.)

**Gotchas:**

- **Don't rely on `Database.URL`'s default.** If `DB_URL` is unset, the server falls back to
  `jdbc:sqlite:file:test?mode=memory&cache=shared` (`server/.../database/Database.kt`) — an **in-memory
  SQLite** database with none of the migrations applied in a way that matches Postgres behavior. It throws
  cryptic `ExposedSQLException: no such table` errors. Always set `DB_URL` explicitly for a standalone run.
- **`KEYS_PATH`/AES key: no manual setup needed.** `security/AES.kt`'s `init()` auto-generates
  `$KEYS_PATH/aes.key` on first run if the directory exists (it creates the directory too) — you'll only see
  `FileNotFoundException: /keys/aes.key` if `KEYS_PATH` itself points somewhere unwritable, or is unset (then
  it defaults to the literal `/keys`, which won't exist outside a container).
- Session cookies need `SECRET_ENCRYPT_KEY`/`SECRET_SIGN_KEY` set to *something* (see `compose.yml` for
  example values) or `plugins/Sessions.kt` will blow up.

### Android client on an emulator

1. **`SERVER_URL` defaults to production** (`https://server.cea.arnaumora.com`, set in
   `composeApp/build.gradle.kts` around the `buildkonfig` block) whenever the env var isn't set at *build*
   time. **This is the single most dangerous gotcha in this repo** — a plain `./gradlew :android:installDebug`
   silently builds an app that talks to the real production server. Always set it explicitly for local
   testing:
   ```shell
   SERVER_URL="http://localhost:8080" ./gradlew :android:installDebug
   ```
   Verify what actually got baked in before trusting a build:
   ```shell
   grep SERVER_URL composeApp/build/generated/source/buildkonfig/androidMain/.../BuildKonfig.kt
   ```
2. **Emulator → host server**: the emulator's `10.0.2.2` alias for the host is flaky in practice; more
   reliable is `adb reverse`:
   ```shell
   adb reverse tcp:8080 tcp:8080
   ```
   then point `SERVER_URL` at `http://localhost:8080` (not `10.0.2.2`).
3. **Cleartext HTTP is blocked by default on Android** — you'll get
   `java.io.IOException: Cleartext HTTP traffic to ... not permitted`. Fix this in the `:android` module's
   **debug-only** source set, not in `:composeApp`'s shared `androidMain` (that would ship cleartext support
   in release builds too):
   - `android/src/debug/res/xml/network_security_config_debug_local.xml` — allow-list `localhost`/`10.0.2.2`
   - `android/src/debug/AndroidManifest.xml` — `<application android:networkSecurityConfig="@xml/network_security_config_debug_local" />`
   Verify it only applies to the debug variant via the merged manifest, not by inspection alone.
4. Install/launch:
   ```shell
   SERVER_URL="http://localhost:8080" ./gradlew :android:installDebug
   adb shell am start -n org.centrexcursionistalcoi.app/.android.MainActivity
   ```
   Note the package/activity split: the Android **package id** is `org.centrexcursionistalcoi.app` (not
   `.android`), but the launcher `MainActivity` lives in the `.android` subpackage. Confirm with:
   ```shell
   adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER org.centrexcursionistalcoi.app
   ```

### Cleaning up

Nothing here auto-stops — when you're done, explicitly tear down what you started:
```shell
docker compose -f compose.yml -f compose.dev.yml down   # or: docker rm -f <container>, if started ad hoc
adb reverse --remove tcp:8080
# kill the :server:run / installDist process if run in the background
```

## 3. Koin dependency injection — current state

**The DI setup is mid-migration and easy to misread from old comments/memory.** The intended, "real" setup
uses the Koin Compiler Plugin with annotations (`@Singleton`, `@KoinViewModel`, `@InjectedParam`,
`@Module @ComponentScan(...)` in `di/AnnotatedModules.kt`) — but that plugin is **currently disabled**:

```kotlin
// composeApp/build.gradle.kts
// TODO: re-enable once Koin supports Kotlin 2.4.20+ -- crashes with an IrGenerationExtensionException
//  (IrUtilsKt.getValueArgument signature changed). Manual replacement for what this plugin generated
//  lives in di/ManualModules.kt. See https://github.com/Centre-Excursionista-Alcoi/App/issues/590
// alias(libs.plugins.koinCompilerPlugin)
```

**What's actually active right now is `di/ManualModules.kt`** — a hand-written `module { }` DSL that mirrors
exactly what the compiler plugin would have generated from the (still-present, inert) `@Singleton`/
`@KoinViewModel`/`@Named`/`@InjectedParam` annotations left on the classes. `di/AnnotatedModules.kt` is a
disabled drop-in target: once issue #590 is resolved, swap `manualModule` back for `AnnotatedModules`'s scan
modules in `di/Koin.kt`'s `initKoin()` and delete `ManualModules.kt`.

**When adding a new injectable class right now: edit `ManualModules.kt` by hand.** The `@Singleton`/
`@KoinViewModel` annotations on the class itself are currently decorative — they do nothing until the plugin
is re-enabled, but keep adding them anyway (so `ManualModules.kt` stays a faithful mirror and the eventual
re-enable is a pure deletion).

**The one gotcha that will silently break things:** Koin's `single(qualifier) { ConcreteJob(...) }` DSL infers
the registered type from the lambda's return type. If code elsewhere resolves the definition by a *different*
(usually base/interface) type — e.g. `BackgroundJobWorker` does
`inject(BackgroundJob::class.java, named(jobName))` while the job is registered as its concrete subclass —
the lookup throws `NoDefinitionFoundException` **at runtime only**, inside a WorkManager worker whose
exceptions don't surface anywhere visible. Compile success and even a naive Koin smoke test prove nothing
here. Fix: append `bind BaseType::class` to the registration. This exact bug shipped and silently broke *all*
background sync app-wide before being caught (see `git log` around `ManualModules.kt`/`TestKoinModules.kt`
for the fix and the regression test).

**`composeApp/src/jvmTest/.../di/TestKoinModules.kt`** is the regression test for the whole graph — it calls
`startKoin` for real and resolves every registered type. When you add a lookup pattern that differs from how
something is *registered* (like the base-type lookup above), add an explicit assertion for that exact lookup
shape, not just the registration shape — a passing test that only re-derives how things were registered
proves nothing new.

## 4. Driving the Android app from the CLI (adb / uiautomator)

There's no `android` CLI wrapper installed in this environment — everything here is plain `adb` +
`uiautomator`, following a **launch → observe → act → re-observe** loop (credit to the shape of
[takahirom/android-cli-ui-automation-skill](https://github.com/takahirom/android-cli-ui-automation-skill),
adapted to plain `adb`).

### Observe

Screenshot (fast, good for visual confirmation, screenshots are worth attaching to PRs):
```shell
adb exec-out screencap -p > /tmp/screen.png
```

Structured UI dump (needed to get **exact tap coordinates** — don't eyeball them from a screenshot, Compose
layouts don't line up with visual guesses):
```shell
adb shell uiautomator dump /sdcard/dump.xml && adb pull /sdcard/dump.xml /tmp/dump.xml
grep -o 'text="Some Label"[^>]*bounds="[^"]*"' /tmp/dump.xml
```
`bounds="[x1,y1][x2,y2]"` — tap the center: `x=(x1+x2)/2`, `y=(y1+y2)/2`.

### Act

```shell
adb shell input tap <x> <y>
adb shell input text "some text"          # no spaces support directly; see gotcha below
adb shell input keyevent KEYCODE_DEL      # backspace
adb shell input keyevent KEYCODE_MOVE_END
adb shell am start -n <pkg>/<activity>
adb shell am force-stop <pkg>
adb shell pm clear <pkg>                  # wipe app data for a clean-slate test
```

### Gotchas hit this session

- **Compose elements are often `clickable="false"` in the accessibility tree** even though tapping them
  works — Compose semantics don't always mark the exact node clickable; the *parent* container handles the
  gesture. Tap the reported bounds' center anyway; don't filter on `clickable="true"` or you'll miss real
  targets. Conversely, use `clickable="true"` nodes to find icon-only buttons (like a dialog's pencil/edit
  icon) that have no `text` to grep for.
- **Bounds shift between renders** (e.g. an error banner appearing pushes a button down). Always re-dump
  after any state change before reusing coordinates — this is the "re-observe" step, and skipping it is the
  single most common cause of a tap silently landing on the wrong thing.
- **A tap that "does nothing" is usually a coordinate miss, not a broken feature.** Before concluding a
  feature is broken, re-dump and re-check bounds.
- **The email/text field on the login and register screens accepts newlines** — pressing Enter mid-automation
  (or an IME "Done" action) can insert a line break instead of submitting, corrupting whatever you typed next
  into the same field. Filed as a real bug (missing `lineLimits = TextFieldLineLimits.SingleLine` on the
  state-based `OutlinedTextField`s in `ui/screen/LoginScreen.kt`) — until fixed, clear the field fully
  (`KEYCODE_MOVE_END` + repeated `KEYCODE_DEL`) rather than trusting a single field to hold exactly what you
  typed.
- **Session/local-storage state persists across app restarts but not across `pm clear`.** If a change made
  server-side (e.g. via direct DB edit — see §6) doesn't show up after restarting the app, don't assume
  caching is broken — first try `pm clear` + fresh login, since some state (like group/role membership) is
  only refreshed on login, not on every app launch.

## 5. Department-scoped RBAC (server + client)

- `DepartmentRole` enum (`:shared`, `data/DepartmentRole.kt`): `ADMIN` (implies every other role),
  `PEOPLE_MANAGER`, `INVENTORY_MANAGER`, `LENDING_MANAGER`, `MEMORY_MANAGER`, `CONTENT_MANAGER` (gates both
  Events and Posts). Stored as a Postgres `text[]` column on `department_members.roles` — not a join table,
  since the role set is small and closed.
- Central permission API: `server/.../security/DepartmentPermissions.kt` —
  `UserSession.hasDepartmentRole(departmentId, role)`, `assertDepartmentRole()`. Route guards go through this
  rather than duplicating "is manager" checks per route.
- Generic CRUD (`routes/RoutesBase.kt`'s `provideEntityRoutes`) enforces permission in **two phases**: a
  coarse `assertMayWriteAtAll()` (global admin, or holds the role in *any* department) runs before touching
  the request body at all, then a fine-grained per-entity check after. This matters for POST specifically —
  the entity doesn't exist yet when the coarse check runs, so a rejected create must roll back cleanly
  (`onWriteRejected` hook) rather than leaving a half-created row.
- Client-side gating mirrors this in two places that are easy to forget one of: (1) list/picker screens must
  filter to departments the viewer actually has the relevant role in, not show everything and rely on the
  server to reject; (2) per-item actions (edit/delete on a specific row) must check the viewer's role in
  *that row's* department specifically, not just "has this role somewhere".
- Granting/changing a member's roles: `PATCH /departments/{id}/members/{memberId}/roles`, gated by
  department `ADMIN` specifically (privilege-escalation-capable). Client UI is
  `ui/dialog/DepartmentMemberRolesDialog.kt`.

## 6. Other gotchas worth knowing upfront

- **Apostrophes/quotes in `strings.xml` do NOT need Android-resource-style backslash escaping** in this
  Compose Multiplatform resources file — a bare `'` is correct. If you see a literal `\'` rendered on screen
  (rather than a clean apostrophe), that's exactly this mistake. There's already a cleanup script for a
  known-affected file: `scripts/clean-backslash.sh` (currently scoped to
  `composeResources/values-ca/strings.xml`) strips stray backslashes before quotes — a translation pass
  or contributor habit from Android-native `strings.xml` conventions is the usual source.
- **Exposed `text[]` (array) columns are lazy JDBC handles.** Reading a `text[]`-backed property (like
  `DepartmentMemberEntity.roles`) outside the `Database { }` transaction block that fetched the row throws
  "object is already closed". Always read array columns inside the same transaction as the fetch.
- **`RedisStoreMap` (If-Modified-Since caching) is a process-lifetime singleton.** Tests that don't isolate
  it can leak cached state between test cases that otherwise look independent — see the fix in
  `server/src/test/.../ApplicationTestBase.kt` if this resurfaces as a flaky/order-dependent test failure.
- **Stacked PRs**: this repo/workflow uses the `gh stack` extension (`gh stack init`, `gh stack add`,
  `gh stack submit`, `gh stack view`, `gh stack checkout`) for PRs that build on an unmerged base branch.
  When a base-branch PR in the stack gets new commits from `master` merged into it (e.g. an unrelated
  hotfix), **you must manually re-merge `master` into each branch in the stack, bottom-up**, and re-push —
  `gh stack sync` manages the stack's own branches, not upstream-`master` drift into the stack's base.
- **`ADMIN_GROUP_NAME` and department roles are separate systems.** Global admin (`ADMIN_GROUP_NAME = "admin"`
  in `:shared`'s `Constants.kt`, stored in `user_references.groups`) is distinct from per-department
  `DepartmentRole.ADMIN`. A global admin implicitly passes every department permission check; a department
  `ADMIN` role only grants control within that one department.

## 7. Testing cheat sheet

```shell
# Whole-graph Koin resolution test (see §3) — run this after any DI change
./gradlew :composeApp:jvmTest --tests "org.centrexcursionistalcoi.app.di.TestKoinModules"

# Compile-only check for the Android source set (fast; catches most breakage without a full install)
./gradlew :composeApp:compileAndroidMain

# Server test suite
./gradlew :server:test

# Full client JVM test suite
./gradlew :composeApp:jvmTest
```

Prefer `:composeApp:compileAndroidMain` over a full `:android:installDebug` when you just need a compile
sanity check — it's a fraction of the time and doesn't touch the emulator.
