package org.morefriends.api

import org.morefriends.models.Place
import org.morefriends.models.Quiz

data class GetCodePostBody (
    val contact: String
)

data class GetQuizPostBody (
    val contact: String,
    val code: String
)

data class QuizPostBody (
    val quiz: Quiz
)

data class QuizUpdatePostBody (
    val token: String,
    val quiz: Quiz
)

data class DeleteQuizPostBody (
    val token: String
)

data class IdeaPostBody (
    val idea: String
)

data class MeetPlacesPostBody (
    val place: Place
)

data class VotePostBody (
    val place: String
)

data class ConfirmPostBody (
    val meet: String,
    val response: Boolean
)

data class MeetProblemPostBody (
    val problem: String
)

data class MeetMessagePostBody (
    val key: String,
    val message: String
)

data class MeetFeedbackPostBody (
    val key: String,
    val feedback: String
)
