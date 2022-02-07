package org.morefriends.services

import org.morefriends.plugins.secrets
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class SendEmail {
    fun send(emailAddress: String, text: String) {
        val props = Properties().also { props ->
            props["mail.smtp.host"] = "smtp.gmail.com"
            props["mail.smtp.socketFactory.port"] = "465"
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.port"] = "465"
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(
                secrets.config.email.from,
                secrets.config.email.password
            )
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(secrets.config.email.from, "MoreFriends.org"))
            message.addRecipient(Message.RecipientType.TO, InternetAddress(emailAddress))
            message.setSubject("Message from MoreFriends.org", "utf-8")
            message.setContent(text, "text/plain")
            Transport.send(message)
        } catch (e: MessagingException) {
            e.printStackTrace()
        }
    }
}
