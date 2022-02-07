package org.morefriends.services

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.morefriends.plugins.secrets

class SendSms {

    init {
        Twilio.init(secrets.config.twilio.sid, secrets.config.twilio.token)
    }

    fun send(phoneNumber: String, text: String) {
        Message.creator(
            PhoneNumber(phoneNumber),
            PhoneNumber(secrets.config.twilio.phoneNumber),
            text
        ).create()
    }
}
