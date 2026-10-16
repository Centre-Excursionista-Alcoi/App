package org.centrexcursionistalcoi.app.ui.preview

import kotlin.io.encoding.Base64

/**
 * A 64x48 PNG of diagonal colored bands, for previews that need a real image: the screens get images from
 * `rememberImageFile(s)`, which needs Koin and the network, so previews pass the bytes in directly.
 */
val PreviewImagePattern: ByteArray by lazy { Base64.decode(PATTERN_BASE64) }

private const val PATTERN_BASE64 =
    "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAwCAIAAAAuKetIAAAAyElEQVR42u3ZsQmFMBSF4TOXUziApfWrHcJS3MDGgd48JhAICTelxY8HUofvb+/R9LvCtxxb+P73HL5z3cP39v9C69NXQuuDAJa+D8DpmwCivgZA9SWAq88BaP0wgKJPXwmtDwJY+j4Ap28CiPoaANWXAK4+B6D1wwCKPn0ltD4IYOn7AJy+CSDqawBUXwK4+hyA1g8DQNcuofVBAO7SKLS+CYBeeYXWlwD0hV3eB7wPeB/wPuB9wPuA9wHvA94HvA94H/jsPvAAW1QiDyHBX6QAAAAASUVORK5CYII="
