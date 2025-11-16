# CEA App

This is the repository for the Centre Excursionista d'Alcoi application.

This app is only intended to be used by members of the club.

## Building

### Server

Build the Docker image with:
```shell
./scripts/build-server.sh
```

## Server Development
### Running Locally

**Required environment variables:** (and their recommended values)
- `APP_VERSION`: `0.0.0`
- `KEYS_PATH`: `./keys`
- `DB_DRIVER`: `org.postgresql.Driver`
- `DB_URL`: `jdbc:postgresql://127.0.0.1:5432/postgres`
- `DB_USER`: `postgres`
- `DB_PASS`: `1234567890abcdef`

**Start the Postgres database:**

```shell
docker compose -f compose.yml -f compose.dev.yml up -d db
```
