package org.morefriends.api

import org.morefriends.models.Attend
import org.morefriends.models.Meet
import org.morefriends.models.Place
import org.morefriends.models.Quiz

data class SuccessApiResponse (
    val ok: Boolean = true
)

data class GetQuizApiResponse (
    val token: String? = null,
    val quiz: Quiz? = null
)

data class MeetAttendanceApiResponse (
    val meet: Meet? = null,
    val places: List<Place>? = null,
    val attend: Attend? = null
)
