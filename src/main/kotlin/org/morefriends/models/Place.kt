package org.morefriends.models

import java.time.Instant

/**
 * A place suggested in a group
 */
class Place : Model() {
    var group: String? = null
    var name: String? = null
    var address: String? = null
    var date: Instant? = null
    var geo: List<Double>? = null
}
