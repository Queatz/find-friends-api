package org.morefriends.db

import org.morefriends.models.*

class PlaceWithVotes {
    var place: Place? = null
    var voted: Boolean? = null
    var votes: Int? = null

}
class QuizWithMet {
    var quiz: Quiz? = null
    var met: List<String>? = null
}

class MeetWithAttendance {
    var meet: Meet? = null
    var place: Place? = null
    var attendees: Int? = null
    var confirm: Confirm? = null
}

class AttendWithQuiz {
    var attend: Attend? = null
    var quiz: Quiz? = null
}

class MeetWithAttendees {
    var meet: Meet? = null
    var attendees: List<AttendWithQuiz>? = null
}
