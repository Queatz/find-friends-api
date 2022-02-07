package org.morefriends.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import org.morefriends.models.Error

fun Application.configureInterceptors() {
    intercept(ApplicationCallPipeline.Monitoring) {
        call.afterFinish {
            if (call.response.status()?.isSuccess() == false) {
                db.insert(Error().apply {
                    url = call.request.uri
                    status = call.response.status()?.value
                    message = call.response.status()?.description
                })
            }
        }

        return@intercept proceed()
    }
}
