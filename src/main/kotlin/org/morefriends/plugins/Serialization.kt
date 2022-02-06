package org.morefriends.plugins

import com.arangodb.velocypack.internal.util.DateUtil
import com.google.gson.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import java.lang.reflect.Type
import java.text.ParseException
import java.time.Instant
import java.util.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(Instant::class.java, InstantTypeConverter())

        }
    }
}

class InstantTypeConverter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(
        src: Instant,
        srcType: Type,
        context: JsonSerializationContext
    ) = JsonPrimitive(DateUtil.format(Date.from(src)))

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ) = try {
        DateUtil.parse(json.asString).toInstant()
    } catch (e: ParseException) {
        null
    }
}
