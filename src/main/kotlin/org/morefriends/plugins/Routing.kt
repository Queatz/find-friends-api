package org.morefriends.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("{ \"ok\": true }", ContentType.Application.Json)
        }

        post("/get-code") {
            call.respondText("{}") // todo: success, fail
        }

        post("/set-code") {
            call.respondText("{}") // todo: your quiz details
        }

        post("/quiz") {
            call.respondText("{}") // todo: success, fail
        }

        post("/quiz/{id}") {
            call.respondText("{}") // todo: update quiz
        }

        post("/idea") {
            call.respondText("{}") // todo: success, fail
        }

        get("/meet/{key}") {
            call.respondText("{}") // todo: return my meet details
        }

        post("/meet/{key}/suggestion") {
            call.respondText("{}") // todo: return my meet details
        }

        post("/meet/{key}") {
            call.respondText("{}") // todo: set my suggestion, return my meet details
        }

        post("/meet/{key}/pass") {
            call.respondText("{}") // todo: remove myself from the meet, return my meet details
        }

        post("/meet/{key}/message") {
            call.respondText("{}") // todo: send message to all attendees, return my meet details
        }

        post("/meet/{key}/problem") {
            call.respondText("{}") // todo: report a problem, return my meet details
        }

        post("/meet/{key}/feedback") {
            call.respondText("{}") // todo: collect feedback, return my meet details
        }
    }
}
