package org.morefriends.models

/**
 * A person attending a meet
 */
class Attend : Model() {
    var key: String? = null
    var meet: String? = null
    var skip: Boolean? = null
}
