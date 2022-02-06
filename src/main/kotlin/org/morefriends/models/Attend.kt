package org.morefriends.models

/**
 * A person attending a meet
 */
class Attend : Model() {
    var key: String? = null
    var group: String? = null
    var quiz: String? = null
    var skip: Boolean? = null
}
