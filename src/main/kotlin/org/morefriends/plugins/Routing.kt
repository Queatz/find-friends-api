package org.morefriends.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.morefriends.*
import org.morefriends.api.*
import org.morefriends.db.Arango
import org.morefriends.models.*
import org.morefriends.services.SendEmail
import org.morefriends.services.SendSms
import java.time.Instant
import java.util.logging.Logger
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
        while (true) {
            // todo: sweep tokens
            // todo: sweep codes
            // todo: schedule meets
            delay(1.hours.inWholeMilliseconds)
        }
    }

    launch {
        // todo delay until Wednesday 10am CST
//        delay(1.minutes.inWholeMilliseconds)

        while (true) {
            Logger.getGlobal().info("Forming groups for ${Instant.now()}")
            formGroups().forEach {
                Logger.getGlobal().info("Group of ${it.size} (${it.map { it.name }.joinToString(" + ")}) formed.")

                it.forEach { quiz ->
                    it.forEach { other ->
                        if (quiz != other) {
                            db.met(quiz.id!!, other.id!!)
                        }
                    }
                }

                createGroup(it)
            }

            delay(1.minutes.inWholeMilliseconds) // delay until Wednesday 10am CST
        }
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

                            val quiz = db.quiz(it.contact.normalizeContact())

                            when {
                                quiz == null -> HttpStatusCode.NotFound.description("A quiz for that contact was not found")
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
                                // todo schedule meets after 30 minutes
                                // todo schedule meets when 50% of unconfirmed people vote for 1 place
                                x.places.firstOrNull { it.votes == x.attendees }?.let {
                                    createMeet(it.place!!)
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

                            // todo, probably something happens here

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

                        // todo: remove any confirms, votes
                        // todo: alert any people going to the same meet that this person is not

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

        post("/attend/{key}/problem") {
            call.receive<MeetProblemPostBody>().also {
                val attend = db.attend(key = call.parameters["key"]!!)

                call.respond(
                    when {
                        attend == null -> HttpStatusCode.NotFound
                        it.problem.isBlank() -> HttpStatusCode.BadRequest.description("Missing problem")
                        else -> {
                            db.insert(Problem().apply {
                                this.meet = attend.id
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
                // todo ensure meet key is supplied as well and matches the meet
                val attend = db.attend(key = call.parameters["key"]!!)

                call.respond(
                    when (attend) {
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

        post("/meet/{id}/feedback") {
            call.receive<MeetFeedbackPostBody>().also {
                // todo ensure meet key is supplied as well and matches the meet
                val meet = db.document(Meet::class, call.parameters["id"]!!)

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

