package org.morefriends.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.morefriends.*
import org.morefriends.api.*
import org.morefriends.db.Arango
import org.morefriends.models.*
import org.morefriends.services.Messaging
import org.morefriends.services.Secrets
import java.time.Instant
import java.util.logging.Logger
import kotlin.random.Random.Default.nextInt

data class Dated<T>(
    val value: T,
    val date: Instant = Instant.now()
)

val db = Arango()
val secrets = Secrets()

val codes = mutableMapOf<String, Dated<String>>()
val tokens = mutableMapOf<String, Dated<String>>()

val messaging = Messaging()

fun Application.configureRouting() {
    routing {
        get("/") { call.respond(SuccessApiResponse()) }

        post("/get-code") {
            call.receive<GetCodePostBody>().also {
                call.respond(
                    when {
                        it.contact.isNotEmpty() -> {
                            val code = nextInt(100000, 999999).toString()
                            val text = "Your MoreFriends.org code is: $code"

                            val quiz = db.quiz(it.contact.normalizeContact())

                            when {
                                quiz == null -> HttpStatusCode.NotFound.description("A quiz for that contact was not found")
                                it.contact.isContact() -> {
                                    messaging.send(it.contact.normalizeContact(), text)
                                    codes[it.contact.normalizeContact()] = Dated(code)
                                    SuccessApiResponse()
                                }
                                else -> HttpStatusCode.BadRequest.description("Contact must be a phone number or email address")
                            }
                        }
                        else -> HttpStatusCode.BadRequest.description("Contact mustn\'t be blank")
                    }
                )
            }
        }

        post("/get-quiz") {
            call.receive<GetQuizPostBody>().also {
                call.respond(
                    when {
                        it.contact.isNotEmpty() && it.code.isNotEmpty() -> {
                            when (it.code) {
                                codes[it.contact.normalizeContact()]?.value -> {
                                    codes.remove(it.contact.normalizeContact())

                                    when (val quiz = db.quiz(contact = it.contact.normalizeContact())) {
                                        null -> HttpStatusCode.NotFound.description("A quiz with the provided contact method does not exist")
                                        else -> {
                                            val token = (0..31).token()
                                            tokens[token] = Dated(quiz.id!!)
                                            GetQuizApiResponse(
                                                token,
                                                quiz
                                            )
                                        }
                                    }
                                }
                                else -> HttpStatusCode.NotFound.description("The code is not valid")
                            }
                        }
                        else -> HttpStatusCode.BadRequest
                    }
                )
            }
        }

        post("/quiz") {
            call.receive<QuizPostBody>().also {
                it.quiz.sanitize()

                call.respond(it.quiz.firstError() ?: db.insert(it.quiz))
            }
        }

        post("/quiz/{id}") {
            call.receive<QuizUpdatePostBody>().also {
                it.quiz.sanitize()

                val existingQuiz = tokens[it.token]?.let {
                    db.document(Quiz::class, it.value)
                }

                it.quiz.id = existingQuiz?.id

                call.respond(
                    when (existingQuiz) {
                        null -> HttpStatusCode.NotFound
                        else -> call.respond(it.quiz.firstError() ?: db.update(it.quiz))
                    }
                )
            }
        }

        post("/quiz/{id}/delete") {
            call.receive<DeleteQuizPostBody>().also {
                val existingQuiz = tokens[it.token]?.let {
                    db.document(Quiz::class, it.value)
                }

                call.respond(
                    when (existingQuiz) {
                        null -> HttpStatusCode.NotFound
                        else -> {
                            // optional: remove active confirms, votes
                            db.delete(existingQuiz)

                            SuccessApiResponse()
                        }
                    }
                )
            }
        }

        post("/idea") {
            call.receive<IdeaPostBody>().also {
                call.respond(
                    when {
                        it.idea.isBlank() -> HttpStatusCode.BadRequest.description("Idea should not be blank")
                        else -> {
                            db.insert(Idea().apply { idea = it.idea })
                            SuccessApiResponse()
                        }
                    }
                )
            }
        }

        get("/attend/{key}") {
            val attend = db.attend(key = call.parameters["key"]!!)

            call.respond(
                when (attend) {
                    null -> HttpStatusCode.NotFound
                    else -> attend.response()
                }
            )
        }

        post("/attend/{key}/places") {
            call.receive<MeetPlacesPostBody>().also {
                val attend = db.attend(key = call.parameters["key"]!!)

                call.respond(
                    when (attend) {
                        null -> HttpStatusCode.NotFound
                        else -> {
                            val group = db.document(Group::class, attend.group!!)

                            if (group == null) HttpStatusCode.NotFound else {
                                it.place.group = attend.group

                                val place = db.insert(it.place)
                                db.vote(attend.id!!, group.id!!, place.id!!)

                                attend.response()
                            }
                        }
                    }
                )
            }
        }

        post("/attend/{key}/vote") {
            val attend = db.attend(key = call.parameters["key"]!!)

            call.receive<VotePostBody>().also {
                call.respond(
                    when (attend) {
                        null -> HttpStatusCode.NotFound
                        else -> {
                            db.vote(attend.id!!, attend.group!!, it.place)

                            attend.response().also { x ->
                                // Send a message for the 1st meet
                                if (db.meets(attend.group!!).isEmpty()) {
                                    x.places.firstOrNull {
                                        it.votes!! >= x.attendees!! / 2
                                    }?.let {
                                        createMeet(it.place!!)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        post("/attend/{key}/confirm") {
            val attend = db.attend(key = call.parameters["key"]!!)

            call.receive<ConfirmPostBody>().also {
                call.respond(
                    when (attend) {
                        null -> HttpStatusCode.NotFound
                        else -> {
                            db.confirm(attend.id!!, it.meet, it.response)

                            db.quizzesConfirmedInMeet(it.meet).filter {
                                it.id != attend.quiz
                            }.also { quizzes ->
                                quizzes.forEach {
                                    messaging.send(it.contact ?: return@forEach, "${quizzes.size} ${if (quizzes.size == 1) "person has" else "people have"} confirmed they are attending your meet")
                                }
                            }

                            attend.response()
                        }
                    }
                )
            }
        }

        post("/attend/{key}/skip") {
            val attend = db.attend(key = call.parameters["key"]!!)

            call.respond(
                when (attend) {
                    null -> HttpStatusCode.NotFound
                    else -> {
                        attend.skip = true
                        db.update(attend)

                        db.removeVotes(attend.id!!)
                        db.removeConfirms(attend.id!!)

                        attend.response()
                    }
                }
            )
        }

        post("/attend/{key}/unskip") {
            val attend = db.attend(key = call.parameters["key"]!!)

            call.respond(
                when (attend) {
                    null -> HttpStatusCode.NotFound
                    else -> {
                        attend.skip = false
                        db.update(attend)

                        attend.response()
                    }
                }
            )
        }

        post("/meet/{id}/problem") {
            call.receive<MeetProblemPostBody>().also {
                val meet = db.document(Meet::class, call.parameters["id"]!!)
                val attend = db.attend(it.key)

                call.respond(
                    when {
                        meet == null -> HttpStatusCode.NotFound
                        attend == null -> HttpStatusCode.NotFound
                        attend.group != meet.group -> HttpStatusCode.NotFound
                        it.problem.isBlank() -> HttpStatusCode.BadRequest.description("Missing problem")
                        else -> {
                            db.insert(Problem().apply {
                                this.meet = meet.id
                                problem = it.problem
                            })

                            SuccessApiResponse()
                        }
                    }
                )
            }
        }

        post("/meet/{id}/message") {
            call.receive<MeetMessagePostBody>().also {
                val meet = db.document(Meet::class, call.parameters["id"]!!)
                val attend = db.attend(it.key)

                call.respond(
                    when {
                        meet == null -> HttpStatusCode.NotFound
                        attend == null -> HttpStatusCode.NotFound
                        attend.group != meet.group -> HttpStatusCode.NotFound
                        else -> {
                            db.quizzesConfirmedInMeet(meet.id!!).filter {
                                it.id != attend.quiz
                            }.forEach { quiz ->
                                Logger.getGlobal().info("Sending message to ${quiz.name}: ${it.message}")
                            }

                            SuccessApiResponse()
                        }
                    }
                )
            }
        }

        post("/meet/{id}/feedback") {
            call.receive<MeetFeedbackPostBody>().also {
                val meet = db.document(Meet::class, call.parameters["id"]!!)
                val attend = db.attend(it.key)

                call.respond(
                    when {
                        meet == null -> HttpStatusCode.NotFound
                        attend == null -> HttpStatusCode.NotFound
                        attend.group != meet.group -> HttpStatusCode.NotFound
                        it.feedback.isBlank() -> HttpStatusCode.BadRequest.description("Missing feedback")
                        else -> {
                            db.insert(Feedback().apply {
                                this.meet = meet.id
                                feedback = it.feedback
                            })

                            SuccessApiResponse()
                        }
                    }
                )
            }
        }
    }
}

private fun Attend.response() = AttendApiResponse(
    this,
    db.document(Quiz::class, quiz!!)?.name,
    db.document(Group::class, group!!)?.geo,
    db.attendees(group!!),
    db.places(group!!, id!!),
    db.meets(group!!, id!!)
)

private fun Quiz.firstError() = when {
    legal?.tos != true -> HttpStatusCode.BadRequest.description("Terms of Service must be accepted")
    legal?.disclaimer != true -> HttpStatusCode.BadRequest.description("Disclaimer must be accepted")
    contact?.isPhoneNumber() != true && contact?.isEmailAddress() != true -> HttpStatusCode.BadRequest.description(
        "Contact should be an email address or phone number"
    )
    city?.isNotBlank() != true -> HttpStatusCode.BadRequest.description("City or town should not be blank")
    geo?.isNotEmpty() != true -> HttpStatusCode.BadRequest.description("Geo coordinates should not be blank")
    distance?.takeIf { it > 0 } == null -> HttpStatusCode.BadRequest.description("Distance should be 0 or greater")
    else -> null
}

private fun Quiz.sanitize() {
    contact = contact?.normalizeContact()
    name = name?.trim()
    city = city?.trim()
}

private fun String.normalizeContact() = when {
    isPhoneNumber() -> normalizePhoneNumber()
    else -> trim()
}

fun Attend.link() = "https://morefriends.org/attend/$key"
