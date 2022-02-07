package org.morefriends.services

import org.morefriends.isEmailAddress
import org.morefriends.isPhoneNumber
import java.util.logging.Logger

class Messaging {

    private val outbox = mutableListOf<Pair<String, String>>()
    private val sendEmail = SendEmail()
    private val sendSms = SendSms()

    fun send(contact: String, text: String) {
        outbox.add(contact to text)
    }

    fun process() {
        while (outbox.isNotEmpty()) {
            outbox.removeFirstOrNull()?.let {
                val contact = it.first
                val text = it.second

                when {
                    contact.isEmailAddress() -> sendEmail.send(contact, text)
                    contact.isPhoneNumber() -> sendSms.send(contact, text)
                    else -> {
                        Logger.getGlobal().info("Failed to send. $contact -x-> $text")
                    }
                }
                Logger.getGlobal().info("$contact -> $text")
            }
        }
    }
}
