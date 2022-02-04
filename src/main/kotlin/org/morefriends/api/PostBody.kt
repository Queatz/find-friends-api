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
    val place: String
)

data class MeetMessagePostBody (
    val message: String
)

data class MeetProblemPostBody (
    val problem: String
)

data class MeetFeedbackPostBody (
    val feedback: String
)
