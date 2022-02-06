package org.morefriends.api

import org.morefriends.db.PlaceWithVotes
import org.morefriends.models.Attend
import org.morefriends.models.Group
import org.morefriends.models.Place
import org.morefriends.models.Quiz

data class SuccessApiResponse (
    val ok: Boolean = true
)

data class GetQuizApiResponse (
    val token: String? = null,
    val quiz: Quiz? = null
)

data class AttendApiResponse (
    val attend: Attend? = null,
    val attendees: Int? = null,
    val places: List<PlaceWithVotes>
)
