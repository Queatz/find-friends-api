package org.morefriends

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.morefriends.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureHTTP()
        configureRouting()
        configureSerialization()
    }.start(wait = true)
}
