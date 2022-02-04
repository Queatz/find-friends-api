package org.morefriends.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*

fun Application.configureHTTP() {
    install(Compression)
    install(DefaultHeaders)
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        allowSameOrigin = true
        allowNonSimpleContentTypes = true
        host("morefriends.org")
        host("morefriends.us")
        host("localhost:4200")
    }
}
