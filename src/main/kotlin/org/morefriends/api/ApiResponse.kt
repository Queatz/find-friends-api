package org.morefriends.api

import org.morefriends.db.MeetWithAttendance
import org.morefriends.db.PlaceWithVotes
import org.morefriends.models.*

data class SuccessApiResponse (
    val ok: Boolean = true
)

data class GetQuizApiResponse (
    val token: String? = null,
    val quiz: Quiz? = null
)

data class AttendApiResponse (
    val attend: Attend? = null,
    val name: String? = null,
    val geo: List<Double>? = null,
    val attendees: Int? = null,
    val places: List<PlaceWithVotes>,
    val meets: List<MeetWithAttendance>,
)
