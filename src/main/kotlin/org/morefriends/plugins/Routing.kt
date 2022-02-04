package org.morefriends.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.morefriends.api.*
import org.morefriends.db.Arango
import org.morefriends.isEmailAddress
import org.morefriends.isPhoneNumber
import org.morefriends.models.Feedback
import org.morefriends.models.Idea
import org.morefriends.models.Problem
import org.morefriends.models.Quiz
import org.morefriends.normalizePhoneNumber
import org.morefriends.services.SendEmail
import org.morefriends.services.SendSms
import org.morefriends.token
import java.time.Instant
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.hours

data class Dated<T>(
    val value: T,
    val date: Instant = Instant.now()
)

val db = Arango()
val codes = mutableMapOf<String, Dated<String>>()
val tokens = mutableMapOf<String, Dated<String>>()

val sendEmail = SendEmail()
val sendSms = SendSms()

@OptIn(DelicateCoroutinesApi::class)
fun Application.configureRouting() {
    launch {
        // todo: sweep tokens
        // todo: sweep codes
        // todo: schedule meets
        delay(1.hours.inWholeMilliseconds)
    }

    routing {
        get("/") { call.respond(SuccessApiResponse()) }

        post("/get-code") {
            call.receive<GetCodePostBody>().also {
                call.respond(
                    when {
                        it.contact.isNotEmpty() -> {
                            val code = nextInt(100000, 999999).toString()
                            val text = "Your morefriends.org code is: $code"

                            when {
                                it.contact.isPhoneNumber() -> {
                                    sendSms.send(it.contact.normalizeContact(), text)
                                    codes[it.contact.normalizeContact()] = Dated(code)
                                    SuccessApiResponse()
                                }
                                it.contact.isEmailAddress() -> {
                                    sendEmail.send(it.contact.normalizeContact(), text)
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
                                        null -> HttpStatusCode.NotFound
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
                                else -> HttpStatusCode.NotFound
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

        post("/quiz/{key}") {
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

        get("/meet/{key}") {
            val meet = db.meet(key = call.parameters["key"]!!)

            call.respond(
                when (meet) {
                    null -> HttpStatusCode.NotFound
                    else -> MeetAttendanceApiResponse(
                        meet,
                        db.places(meet = meet.id!!),
                        db.attend(key = call.parameters["key"]!!)
                    )
                }
            )
        }

        post("/meet/{key}/places") {
            call.receive<MeetPlacesPostBody>().also {
                val meet = db.meet(key = call.parameters["key"]!!)

                call.respond(
                    when (meet) {
                        null -> HttpStatusCode.NotFound
                        else -> {
                            it.place.meet = meet.id

                            db.insert(it.place)

                            // todo: automatically vote for this place

                            MeetAttendanceApiResponse(
                                meet,
                                db.places(meet = meet.id!!),
                                db.attend(key = call.parameters["key"]!!)
                            )
                        }
                    }
                )
            }
        }

        post("/meet/{key}/vote") {
            val meet = db.meet(key = call.parameters["key"]!!)
            val attend = db.attend(key = call.parameters["key"]!!)

            call.receive<VotePostBody>().also {
                call.respond(
                    when {
                        meet == null -> HttpStatusCode.NotFound
                        attend == null -> HttpStatusCode.NotFound
                        else -> {
                            db.vote(attend.id!!, meet.id!!, it.place)

                            MeetAttendanceApiResponse(
                                meet,
                                db.places(meet = meet.id!!),
                                db.attend(key = call.parameters["key"]!!)
                            )
                        }
                    }
                )
            }
        }

        post("/meet/{key}/confirm") {
            val meet = db.meet(key = call.parameters["key"]!!)
            val attend = db.attend(key = call.parameters["key"]!!)

            call.receive<ConfirmPostBody>().also {
                call.respond(
                    when {
                        meet == null -> HttpStatusCode.NotFound
                        attend == null -> HttpStatusCode.NotFound
                        else -> {
                            db.confirm(attend.id!!, meet.id!!, it.place)

                            MeetAttendanceApiResponse(
                                meet,
                                db.places(meet = meet.id!!),
                                db.attend(key = call.parameters["key"]!!)
                            )
                        }
                    }
                )
            }
        }

        post("/meet/{key}/skip") {
            val attend = db.attend(key = call.parameters["key"]!!)

            call.respond(
                when (attend) {
                    null -> HttpStatusCode.NotFound
                    else -> {
                        attend.skip = true

                        db.update(attend)

                        SuccessApiResponse()
                    }
                }
            )
        }

        post("/meet/{key}/message") {
            call.receive<MeetMessagePostBody>().also {
                val meet = db.meet(key = call.parameters["key"]!!)

                call.respond(
                    when (meet) {
                        null -> HttpStatusCode.NotFound
                        else -> {
                            // todo: send message to people scheduled for my same meet location
                            // todo: it.message
                            SuccessApiResponse()
                        }
                    }
                )
            }
        }

        post("/meet/{key}/problem") {
            call.receive<MeetProblemPostBody>().also {
                val meet = db.meet(key = call.parameters["key"]!!)

                call.respond(
                    when {
                        meet == null -> HttpStatusCode.NotFound
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

        post("/meet/{key}/feedback") {
            call.receive<MeetFeedbackPostBody>().also {
                val meet = db.meet(key = call.parameters["key"]!!)

                call.respond(
                    when {
                        meet == null -> HttpStatusCode.NotFound
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

