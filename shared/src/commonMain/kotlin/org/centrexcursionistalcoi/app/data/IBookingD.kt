package org.centrexcursionistalcoi.app.data

interface IBookingD : DatabaseData {
    val from: Long?
    val to: Long?
    val userId: String?
    val confirmed: Boolean
    val takenAt: Long?
    val returnedAt: Long?
}
