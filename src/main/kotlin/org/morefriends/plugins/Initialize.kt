package org.morefriends.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.morefriends.createGroup
import org.morefriends.formGroups
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters.next
import java.time.temporal.TemporalUnit
import java.util.logging.Logger
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

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
        delayUntil(ZonedDateTime.now(ZoneId.of("America/Chicago"))
            .with(next(DayOfWeek.WEDNESDAY))
            .truncatedTo(ChronoUnit.DAYS)
            .withHour(10)
            .toInstant())

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

            delayUntil(ZonedDateTime.now(ZoneId.of("America/Chicago"))
                .with(next(DayOfWeek.WEDNESDAY))
                .truncatedTo(ChronoUnit.DAYS)
                .withHour(10)
                .toInstant())
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

suspend fun delayUntil(instant: Instant) {
    delay((instant.toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0))
}

private suspend fun delayUntilNext(unit: TemporalUnit, count: Long) {
    delay((Instant.now().truncatedTo(unit).plusSeconds(count).toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0))
}
