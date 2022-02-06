package org.morefriends.db

import org.morefriends.models.Group
import org.morefriends.models.Meet
import org.morefriends.models.Place
import org.morefriends.models.Quiz

class PlaceWithVotes {
    var place: Place? = null
    var voted: Boolean? = null
    var votes: Int? = null

}
class QuizWithMet {
    var quiz: Quiz? = null
    var met: List<String>? = null

}

class GroupDetailsResult {
    var group: Group? = null
    var people: Int? = null
    var places: List<Place>? = null
}

class MeetDetailsResult {
    var meet: Meet? = null
    var attendees: Int? = null
    var place: Place? = null
}
