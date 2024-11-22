# Centre Excursionista d'Alcoi App

[![Backend Status](https://status.escalaralcoiaicomtat.org/api/badge/9/status?style=for-the-badge)](https://status.escalaralcoiaicomtat.org/status/cea)

The image is hosted at [Docker Hub](https://hub.docker.com/repository/docker/arnyminerz/cea-app/general).

# Server Configuration

The server is configured with the following environment variables:

**Database:**
- `DATABASE_URL`: The URL of the database. Default: `jdbc:h2:file:./CEA`
- `DATABASE_DRIVER`: The driver of the database. Default: `org.h2.Driver`
- `DATABASE_USERNAME`: The username of the database. Default: ``
- `DATABASE_PASSWORD`: The password of the database. Default: ``
**FCM:**
- `FCM_SERVICE_ACCOUNT_KEY_PATH`: The path to the service account key. Default: `./serviceAccountKey.json`
**Sessions:**
- `REDIS_ENDPOINT`: The endpoint of the Redis server. Default: `null` (in-memory sessions)
- `SESSION_DURATION`: The amount of time in minutes that a session is valid. Default: `1440` (1 day)

# License

<small><a href="https://github.com/Centre-Excursionista-Alcoi/App/blob/master/LICENSE">Full text</a></small>

<p><a property="dct:title" rel="cc:attributionURL" href="https://github.com/Centre-Excursionista-Alcoi/App">CEA App</a> by <a rel="cc:attributionURL dct:creator" property="cc:attributionName" href="https://arnyminerz.com">Arnau Mora Gras</a> is licensed under <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/?ref=chooser-v1" target="_blank" rel="license noopener noreferrer" style="display:inline-block;">CC BY-NC-SA 4.0<img style="height:22px!important;margin-left:3px;vertical-align:text-bottom;" src="https://mirrors.creativecommons.org/presskit/icons/cc.svg?ref=chooser-v1" alt=""><img style="height:22px!important;margin-left:3px;vertical-align:text-bottom;" src="https://mirrors.creativecommons.org/presskit/icons/by.svg?ref=chooser-v1" alt=""><img style="height:22px!important;margin-left:3px;vertical-align:text-bottom;" src="https://mirrors.creativecommons.org/presskit/icons/nc.svg?ref=chooser-v1" alt=""><img style="height:22px!important;margin-left:3px;vertical-align:text-bottom;" src="https://mirrors.creativecommons.org/presskit/icons/sa.svg?ref=chooser-v1" alt=""></a></p> 
