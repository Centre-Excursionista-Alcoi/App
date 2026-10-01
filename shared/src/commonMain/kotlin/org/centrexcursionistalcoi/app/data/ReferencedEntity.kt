package org.centrexcursionistalcoi.app.data

interface ReferencedEntity<IdType : Any, Original : Entity<IdType>>: Entity<IdType> {
    fun dereference(): Original
}
