package org.morefriends.services

import java.util.logging.Logger

class SendSms {
    fun send(phoneNumber: String, text: String) {
        Logger.getGlobal().info("$phoneNumber = $text")
    }
}
