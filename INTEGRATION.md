# FEMECV Integration

When you introduce your FEMECV credentials into the app, you authorize the Centre Excursionista d'Alcoi to store and
process your FEMECV data.

Specifically, this means storing your credentials using AES/CBC encryption with `PKCS5` Padding: [see implementation](https://github.com/Centre-Excursionista-Alcoi/App/blob/master/server/src/main/kotlin/org/centrexcursionistalcoi/app/security/AES.kt).

The app will fetch your data the first time you log in, and then periodically (every 7 days) to keep your information up to date.

This data will be stored securily on the app's servers.
