package org.centrexcursionistalcoi.app.authentik.errors

import java.io.IOException

class AuthentikException(val error: AuthentikError): IOException("Authentik API error occurred. Code: ${error.code}")
