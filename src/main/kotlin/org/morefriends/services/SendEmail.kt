package org.morefriends.services

import java.util.logging.Logger

class SendEmail {
    fun send(emailAddress: String, text: String) {
        Logger.getGlobal().info("$emailAddress = $text")
    }
}
