package org.morefriends

import org.morefriends.models.Quiz
import org.morefriends.plugins.db
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


fun formGroups(): List<Set<Quiz>> {
    val quizzes = db.activeQuizzes()

    // Find potential groups, sorted from largest to smallest
    val groups = quizzes.map { quiz ->
        val group = (quizzes.filter { it != quiz && quiz.hasPotential(it) } + quiz).toMutableSet()

        group
    }.filter { it.size >= 2 }.sortedByDescending { it.size }

    val alreadyGrouped = mutableSetOf<Quiz>()

    // Put people in the largest group for them, and remove them from all other groups
    return groups.onEach {
        it.removeAll(alreadyGrouped)
        alreadyGrouped.addAll(it)
    }.filter { it.size >= 2 }
}

internal fun Quiz.hasPotential(other: Quiz): Boolean {
    val d = distance(geo!![0], geo!![1], other.geo!![0], other.geo!![1])

    // Check distance and required identical questions
    if(
        d > distance!!
        || (
                !friendDetails!!.includeWhenAbsent!!
                        && (
                                !friendDetails!!.genders!!.emptyOrHas(other.personDetails!!.gender) ||
                                !friendDetails!!.ages!!.emptyOrHas(other.personDetails!!.age) ||
                                !friendDetails!!.married!!.emptyOrHas(other.personDetails!!.married) ||
                                !friendDetails!!.kids!!.emptyOrHasAll(other.personDetails!!.kids!!) ||
                                !friendDetails!!.pets!!.emptyOrHas(other.personDetails!!.pets)
                        )
                )
        || friendFacts!!.anyAreRequiredAndNotEqual(other.friendFacts!!)
        || friendScenarios!!.anyAreRequiredAndNotEqual(other.friendScenarios!!)
        || meetPreferences!!.anyAreRequiredAndNotEqual(other.meetPreferences!!)
        || meetPlaces!!.anyAreRequiredAndNotEqual(other.meetPlaces!!)
    ) return false

    // Check similarity
    val similar =
        friendFacts!!.entries.map { other.friendFacts?.get(it.key)?.choice == it.value.choice } +
                friendScenarios!!.entries.map { other.friendScenarios?.get(it.key)?.choice == it.value.choice } +
                meetPreferences!!.entries.map { other.meetPreferences?.get(it.key)?.choice == it.value.choice } +
                meetPlaces!!.entries.map { other.meetPlaces?.get(it.key)?.choice == it.value.choice } +
                friendDetails!!.genders!!.emptyOrHas(other.personDetails!!.gender) +
                friendDetails!!.ages!!.emptyOrHas(other.personDetails!!.age) +
                friendDetails!!.married!!.emptyOrHas(other.personDetails!!.married) +
                friendDetails!!.kids!!.emptyOrHasAny(other.personDetails!!.kids!!) +
                friendDetails!!.pets!!.emptyOrHas(other.personDetails!!.pets)


    val similarity = similar.count { it }.toDouble() / similar.size.toDouble()

    return similarity * 100 >= minimumSimilarity!!
}

fun distance(aLatitude: Double, aLongitude: Double, bLatitude: Double, bLongitude: Double, inMeters: Boolean = false): Double {
    val latDiff = Math.toRadians(bLatitude - aLatitude)
    val lngDiff = Math.toRadians(bLongitude - aLongitude)
    val a = sin(latDiff / 2) * sin(latDiff / 2) +
            cos(Math.toRadians(aLatitude)) * cos(Math.toRadians(bLatitude)) *
            sin(lngDiff / 2) * sin(lngDiff / 2)
    val distance = 3958.75 * (2 * atan2(sqrt(a), sqrt(1 - a)))
    return if (inMeters) distance * 1609.0 else distance
}
