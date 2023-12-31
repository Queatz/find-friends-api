package org.morefriends.db

import com.arangodb.ArangoDB
import com.arangodb.ArangoDBException
import com.arangodb.ArangoDatabase
import com.arangodb.DbName
import com.arangodb.entity.CollectionType
import com.arangodb.entity.EdgeDefinition
import com.arangodb.model.CollectionCreateOptions
import com.arangodb.model.DocumentCreateOptions
import com.arangodb.model.DocumentUpdateOptions
import com.arangodb.velocypack.module.jdk8.VPackJdk8Module
import org.morefriends.models.*
import java.time.Instant
import kotlin.reflect.KClass

class Arango {
    private val db = ArangoDB.Builder()
        .user("friends")
        .password("friends")
        .registerModule(VPackJdk8Module())
        .build()
        .db(DbName.of("friends"))
        .setup()

    private fun <T : Model> one(klass: KClass<T>, query: String, parameters: Map<String, Any> = mapOf()) =
        db.query(
            query,
            if (query.contains("@@collection")) mutableMapOf("@collection" to klass.dbCollection()) + parameters else parameters,
            klass.java
        ).stream().findFirst().takeIf { it.isPresent }?.get()

    private fun <T : Model> list(klass: KClass<T>, query: String, parameters: Map<String, Any> = mapOf()) =
        db.query(
            query,
            if (query.contains("@@collection")) mutableMapOf("@collection" to klass.dbCollection()) + parameters else parameters,
            klass.java
        ).asListRemaining().toList()

    private fun <T : Any> query(klass: KClass<T>, query: String, parameters: Map<String, Any> = mapOf()) =
        db.query(
            query,
            parameters,
            klass.java
        ).asListRemaining().toList()

    fun <T : Model>insert(model: T) = db.collection(model::class.dbCollection()).insertDocument(model.apply { createdAt = Instant.now() }, DocumentCreateOptions().returnNew(true))!!.new!!
    fun <T : Model>update(model: T) = db.collection(model::class.dbCollection()).updateDocument(model.id?.asKey(), model, DocumentUpdateOptions().returnNew(true))!!.new!!
    fun <T : Model>delete(model: T) = db.collection(model::class.dbCollection()).deleteDocument(model.id?.asKey())!!

    fun <T : Model> document(klass: KClass<T>, id: String) = try {
        db.collection(klass.dbCollection()).getDocument(id.asKey(), klass.java)
    } catch (e: ArangoDBException) {
        null
    }

    fun quiz(contact: String) = one(
        Quiz::class, """
            for quiz in @@collection
                filter quiz.contact == @contact
                return quiz
        """, mapOf(
            "contact" to contact
        )
    )

    fun activeQuizzes() = query(
        QuizWithMet::class, """
            for quiz in ${ DbCollection.Quiz.dbCollection() }
                filter quiz.paused != true
                return {
                    quiz: quiz,
                    met: (for met in outbound quiz graph `${ DbCollection.Met.dbGraph() }` return met._id)
                }
        """
    )

    fun attend(key: String) = one(
        Attend::class, """
            for attend in @@collection
                filter attend.key == @key
                return attend
        """, mapOf(
            "key" to key
        )
    )

    fun places(group: String, attend: String) = query(
        PlaceWithVotes::class, """
            for place in ${ DbCollection.Place.dbCollection() }
                filter place.group == @group
                let votes = count(
                        for vote in ${ DbCollection.Vote.dbCollection() }
                            filter vote.place == place._id
                            return vote
                    )
                sort votes desc, date_timestamp(place.createdAt) asc
                return {
                    place: place,
                    voted: count(
                        for vote in ${ DbCollection.Vote.dbCollection() }
                            filter vote.place == place._id and vote.attend == @attend
                            return vote
                    ) == 1,
                    votes: votes
                }
        """, mapOf(
            "group" to group,
            "attend" to attend
        )
    )

    fun vote(attend: String, group: String, place: String) = one(
        Vote::class, """
            upsert { attend: @attend, group: @group }
                insert { attend: @attend, group: @group, place: @place, createdAt: date_iso8601(date_now()) }
                update { place: @place }
                in @@collection
                return NEW
        """, mapOf(
            "attend" to attend,
            "group" to group,
            "place" to place
        )
    )

    fun confirm(attend: String, meet: String, response: Boolean) = one(
        Confirm::class, """
            upsert { attend: @attend, meet: @meet }
                insert { attend: @attend, meet: @meet, response: @response , createdAt: date_iso8601(date_now()) }
                update { response: @response }
                in @@collection
                return NEW
        """, mapOf(
            "attend" to attend,
            "meet" to meet,
            "response" to response
        )
    )

    fun met(quiz: String, otherQuiz: String) = one(
        Met::class, """
            upsert { _from: @from, _to: @to }
                insert { _from: @from, _to: @to, createdAt: date_iso8601(date_now()) }
                update { }
                in @@collection
                return NEW
        """, mapOf(
            "from" to quiz,
            "to" to otherQuiz
        )
    )

    fun attendees(group: String) = query(Int::class, """
        return count (
            for attend in ${ DbCollection.Attend.dbCollection() }
                filter attend.group == @group
                return attend
        )
    """, mapOf(
        "group" to group
    )
    ).firstOrNull() ?: 0

    fun unconfirmedQuizzesInGroup(group: String) = query(
        AttendWithQuiz::class, """
        for attend in ${DbCollection.Attend.dbCollection()}
            filter attend.skip != true
                and attend.group == @group
                and count(
                    for confirm in ${DbCollection.Confirm.dbCollection()}
                        filter confirm.attend == attend._id
                            and confirm.response == true
                        return confirm
                ) == 0
            return {
                attend: attend,
                quiz: document(attend.quiz)
            }
        """, mapOf(
            "group" to group
        )
    )

    fun meets(group: String, attend: String) = query(
        MeetWithAttendance::class, """
            for meet in ${ DbCollection.Meet.dbCollection() }
                filter meet.group == @group
                sort date_timestamp(meet.createdAt) asc
                return {
                    meet: meet,
                    place: document(meet.place),
                    confirm: first(
                        for confirm in ${ DbCollection.Confirm.dbCollection() }
                            filter confirm.meet == meet._id and confirm.attend == @attend
                            return confirm
                    ),
                    attendees: count(
                        for confirm in ${ DbCollection.Confirm.dbCollection() }
                            filter confirm.meet == meet._id and confirm.response == true
                            return confirm
                    )
                }
        """, mapOf(
            "group" to group,
            "attend" to attend,
        )
    )

    fun meets(group: String) = list(
        Meet::class,
        """
            for meet in @@collection
                filter meet.group == @group
                return meet
        """, mapOf(
            "group" to group
        )
    )

    fun quizzesConfirmedInMeet(meet: String) = list(
        Quiz::class,
        """
            for confirm in ${Confirm::class.dbCollection()}
                filter confirm.meet == @meet
                return document(
                    document(confirm.attend).quiz
                )
        """, mapOf(
            "meet" to meet
        )
    )

    fun meetAtPlace(group: String, place: String) = one(
        Meet::class,
        """
            for meet in @@collection
                filter meet.group == @group
                    and meet.place == @place
                return meet
        """, mapOf(
            "group" to group,
            "place" to place
        )
    )

    fun removeVotes(attend: String) = query(
        Vote::class, """
            for vote in @@collection
                    filter vote.attend == @attend
                remove vote in @@collection
        """, mapOf(
            "attend" to attend
        )
    )

    fun removeConfirms(attend: String) = query(
        Confirm::class, """
            for confirm in @@collection
                    filter confirm.attend == @attend
                remove confirm in @@collection
        """, mapOf(
            "attend" to attend
        )
    )

    fun meetsStartingBetween(after: Instant, atOrBefore: Instant) = query(
        MeetWithAttendees::class,
        """
            for meet in ${DbCollection.Meet.dbCollection()}
                for place in ${DbCollection.Place.dbCollection()}
                        filter place._id == meet.place
                            and place.date > @after
                            and place.date <= @atOrBefore
                    return {
                        meet: meet,
                        attendees: (
                            for attend in ${DbCollection.Attend.dbCollection()}
                                for confirm in ${DbCollection.Confirm.dbCollection()}
                                        filter confirm.attend == attend._id
                                            and confirm.meet == meet._id
                                            and confirm.response == true
                                    return {
                                        attend: attend,
                                        quiz: document(attend.quiz)
                                    }
                        )
                    }
        """,
        mapOf(
            "after" to after.toEpochMilli(),
            "atOrBefore" to atOrBefore.toEpochMilli()
        )
    )
}

private fun ArangoDatabase.setup() = apply {
    DbCollection.values().forEach { collection ->
        try {
            createCollection(collection.dbCollection(), CollectionCreateOptions().type(
                if (collection.isEdges) CollectionType.EDGES else CollectionType.DOCUMENT
            ))
        } catch (ignored: ArangoDBException) {
            // Most likely exists
        }

        try {
            if (collection.isEdges) {
                createGraph(
                    "${collection.dbCollection()}-graph", listOf(
                        EdgeDefinition().collection(collection.dbCollection())
                            .from(*collection.nodes.map { it.dbCollection() }.toTypedArray())
                            .to(*collection.nodes.map { it.dbCollection() }.toTypedArray())
                    )
                )
            }
        } catch (ignored: ArangoDBException) {
            // Most likely exists
        }
    }
}

private fun String.asKey() = this.split("/").last()

private fun <T : Model> KClass<T>.dbCollection() = DbCollection.values().first { this == it.model }.dbCollection()

private fun DbCollection.dbCollection() = name.lowercase()
private fun DbCollection.dbGraph() = "${dbCollection()}-graph"
