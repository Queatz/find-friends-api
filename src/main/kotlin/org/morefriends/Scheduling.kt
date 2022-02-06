package org.morefriends

import org.morefriends.models.Attend
import org.morefriends.models.Group
import org.morefriends.models.Quiz
import org.morefriends.plugins.db

fun createGroup(quizzes: Collection<Quiz>) {
    val group = db.insert(Group())

    quizzes.map {
        val attend = Attend().apply {
            this.group = group.id
            quiz = it.id
            key = (0 until 16).token()
        }

        db.insert(attend)
    }.forEach {
        // todo send SMS + Email
        // todo " We've grouped you with 4 people that are potentially great friends for you. "
    }
}
