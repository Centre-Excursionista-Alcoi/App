package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
sealed class ReferencedEntity<IdType : Any, Original : Entity<IdType>>: Entity<IdType> {
    abstract val referencedEntity: Original

    fun dereference(): Original = referencedEntity

    final override fun toMap(): Map<String, Any?> = referencedEntity.toMap()
}
