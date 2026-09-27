# CEA App

This is the repository for the Centre Excursionista d'Alcoi application.

This app is only intended to be used by members of the club.

[Documentation](https://centre-excursionista-alcoi.github.io/App/)

## Building

### Server

Build the Docker image with:
```shell
./scripts/build-server.sh
```

## iOS Keychain Tests

On an Apple Silicon Mac with Xcode, boot an iOS simulator before running:

```shell
SERVER_URL="http://localhost:8080" ./gradlew :composeApp:iosSimulatorArm64Test
```

To select a specific booted simulator, append `--device <simulator UUID>` (list devices with
`xcrun simctl list devices`). The test task embeds a dedicated Keychain entitlement and disables
standalone execution, which cannot access the Keychain service. Tests use unique service names and
clean up their own entries, leaving app credentials untouched.

## Server Development
### Running Locally

**Required environment variables:** (and their recommended values)
- `APP_VERSION`: `0.0.0`
- `KEYS_PATH`: `./keys`
- `DB_DRIVER`: `org.postgresql.Driver`
- `DB_URL`: `jdbc:postgresql://127.0.0.1:5432/postgres`
- `DB_USER`: `postgres`
- `DB_PASS`: `1234567890abcdef`
- `ENV`: `development`

Files are stored in Cloudflare R2 (or any S3-compatible storage) when these are set, and in the local directory
`FILES_PATH` (default `./files`) otherwise. Only development servers (`ENV=development`) may store files locally:
the server refuses to start otherwise.
- `S3_ENDPOINT`: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`
- `S3_BUCKET`: the bucket's name
- `S3_ACCESS_KEY_ID` and `S3_SECRET_ACCESS_KEY`: an R2 API token with *Object Read & Write* on the bucket
- `S3_REGION`: `auto` (the default)

**Start the Postgres database:**

```shell
docker compose -f compose.yml -f compose.dev.yml up -d db
```
