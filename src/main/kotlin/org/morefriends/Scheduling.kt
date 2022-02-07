package org.morefriends

import org.morefriends.models.*
import org.morefriends.plugins.db
import org.morefriends.plugins.link
import org.morefriends.plugins.messaging

fun createGroup(quizzes: Collection<Quiz>) {
    val group = db.insert(Group().apply {
        geo = listOf(
            quizzes.map { it.geo!![0] }.average(),
            quizzes.map { it.geo!![1] }.average()
        )
    })

    quizzes.map {
        val attend = Attend().apply {
            this.group = group.id
            quiz = it.id
            key = (0 until 16).token()
        }

        it to db.insert(attend)
    }.forEach {
        messaging.send(it.first.contact ?: return@forEach, "Dear ${it.first.name ?: "human"}, we've grouped you with ${quizzes.size - 1} ${if (quizzes.size - 1 == 1) "person" else "people"} that are potentially great friends for you.")
        messaging.send(it.first.contact ?: return@forEach, "Click the link below to suggest a meeting time and place.\n${it.second.link()}")
    }
}

fun createMeet(place: Place) {
    val existing = db.meetAtPlace(place.group!!, place.id!!)

    if (existing != null) {
        return
    }

    val meet = Meet().apply {
        this.place = place.id
        this.group = place.group
    }

    db.insert(meet)

    db.unconfirmedQuizzesInGroup(place.group!!).forEach {
        messaging.send(it.quiz!!.contact ?: return@forEach, "Dear ${it.quiz!!.name ?: "human"}, a meet has been scheduled for your group.")
        messaging.send(it.quiz!!.contact ?: return@forEach, "Click the link below to confirm if you are attending.\n${it.attend!!.link()}")
    }
}
