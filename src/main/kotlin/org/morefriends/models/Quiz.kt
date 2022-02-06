package org.morefriends.models

/**
 * A quiz
 */
class Quiz : Model() {
    var name: String? = null
    var city: String? = null
    var geo: List<Double>? = null
    var contact: String? = null
    var distance: Double? = null
    var personDetails: PersonDetails? = null
    var friendDetails: FriendDetails? = null
    var minimumSimilarity: Double? = null
    var friendFacts: Map<String, Choice>? = null
    var friendScenarios: Map<String, Choice>? = null
    var meetPreferences: Map<String, Choice>? = null
    var meetPlaces: Map<String, Choice>? = null
    var legal: Legal? = null
    var paused: Boolean? = null
}

class FriendDetails {
    var genders: List<String>? = null
    var ages: List<String>? = null
    var married: List<String>? = null
    var kids: List<String>? = null
    var pets: List<String>? = null
    var includeWhenAbsent: Boolean? = null
}

class PersonDetails {
    var gender: String? = null
    var age: String? = null
    var married: String? = null
    var kids: List<String>? = null
    var pets: String? = null
}

class Legal {
    var disclaimer: Boolean? = null
    var tos: Boolean? = null
}

class Choice {
    var choice: String? = null
    var required: Boolean? = null
}
