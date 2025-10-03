# Auth Reverse Proxy

This project uses cookies to persist authentication. Most modern web browsers require that the cookies are secure,
meaning they must be sent over HTTPS. Therefore, to use this reverse proxy effectively, you need to set up HTTPS.

To make sure the cookies are secure, the `SameSite` attribute is often required as well, which is not allowed when using
HTTP. This is why HTTPS is necessary.

To start the containers run:
```shell
docker compose up -d
```
