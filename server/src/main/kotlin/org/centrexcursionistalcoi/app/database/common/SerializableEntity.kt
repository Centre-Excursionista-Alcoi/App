package org.centrexcursionistalcoi.app.database.common

import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.data.Serializable

interface SerializableEntity<SerializableType : DatabaseData>: Serializable<SerializableType>
