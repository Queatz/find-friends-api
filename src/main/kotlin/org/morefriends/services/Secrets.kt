package org.morefriends.services

import com.google.gson.Gson
import java.io.File

class Secrets {

    val config: SecretsConfig by lazy {
        Gson().fromJson(File("./secrets.json").reader(), SecretsConfig::class.java)
    }

}

data class SecretsConfig(
    val twilio: TwilioSecretsConfig,
    val email: EmailSecretsConfig
)

data class EmailSecretsConfig(
    val from: String,
    val password: String,
)

data class TwilioSecretsConfig(
    val sid: String,
    val token: String,
    val phoneNumber: String
)
