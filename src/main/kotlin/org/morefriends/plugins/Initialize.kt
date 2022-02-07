package org.morefriends.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.morefriends.createGroup
import org.morefriends.formGroups
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.logging.Logger
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(DelicateCoroutinesApi::class)
fun Application.initialize() {
    launch {
        delayUntilNext(ChronoUnit.SECONDS, 1)

        while (isActive) {
            val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS)
            codes.entries.forEach {
                if (it.value.date.isBefore(oneHourAgo)) {
                    codes.remove(it.key)
                }
            }

            val oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS)
            tokens.entries.forEach {
                if (it.value.date.isBefore(oneDayAgo)) {
                    tokens.remove(it.key)
                }
            }

            delay(1.hours.inWholeMilliseconds)
        }
    }

    launch {
        // optional: delay until Wednesday 10am CST
        delayUntilNext(ChronoUnit.HOURS, 1)

        while (isActive) {
            Logger.getGlobal().info("Forming groups for ${Instant.now()}")
            formGroups().forEach {
                Logger.getGlobal().info("Group of ${it.size} (${it.map { it.name }.joinToString(" + ")}) formed.")

                it.forEach { quiz ->
                    it.forEach { other ->
                        if (quiz != other) {
                            db.met(quiz.id!!, other.id!!)
                        }
                    }
                }

                createGroup(it)
            }

            delay(1.minutes.inWholeMilliseconds) // delay until Wednesday 10am CST
        }

    }

    launch {
        var lastTick = Instant.now()

        delayUntilNext(ChronoUnit.MINUTES, 1)

        while (isActive) {
            val tick = Instant.now()

            db.meetsStartingBetween(lastTick.plus(2, ChronoUnit.HOURS), tick.plus(2, ChronoUnit.HOURS)).forEach { meets ->
                meets.attendees?.forEach {
                    messaging.send(it.quiz?.contact ?: return@forEach, "Dear ${it.quiz?.name ?: "human"}, your scheduled meet with ${meets.attendees!!.size - 1} ${if (meets.attendees!!.size - 1 == 1) "person" else "people"} is starting in 2 hours.")
                    messaging.send(it.quiz?.contact ?: return@forEach, "Click the link below to review the time and place.\n${it.attend!!.link()}")
                }
            }

            lastTick = tick

            delay(1.hours.inWholeMilliseconds)
        }
    }

    launch {
        delayUntilNext(ChronoUnit.SECONDS, 1)

        while (isActive) {
            messaging.process()

            delay(1.seconds.inWholeMilliseconds)
        }
    }
}

private suspend fun CoroutineScope.delayUntilNext(unit: TemporalUnit, count: Long) {
    delay((Instant.now().truncatedTo(unit).plusSeconds(count).toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0))
}
