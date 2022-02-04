package org.morefriends.db

import com.arangodb.ArangoDB
import com.arangodb.ArangoDBException
import com.arangodb.ArangoDatabase
import com.arangodb.DbName
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

    private fun <T : Model> one(klass: KClass<T>, query: String, parameters: Map<String, String> = mapOf()) =
        db.query(
            query,
            mutableMapOf("@collection" to klass.dbCollection()) + parameters,
            klass.java
        ).stream().findFirst().takeIf { it.isPresent }?.get()

    private fun <T : Model> list(klass: KClass<T>, query: String, parameters: Map<String, String> = mapOf()) =
        db.query(
            query,
            mutableMapOf("@collection" to klass.dbCollection()) + parameters,
            klass.java
        ).asListRemaining().toList()

    fun insert(model: Model) = db.collection(model::class.dbCollection()).insertDocument(model.apply { createdAt = Instant.now() }, DocumentCreateOptions().returnNew(true))!!.new
    fun update(model: Model) = db.collection(model::class.dbCollection()).updateDocument(model.id?.asKey(), model, DocumentUpdateOptions().returnNew(true))!!.new

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

    fun meet(key: String) = one(
        Meet::class, """
            for attend in @@collection
                filter attend.key == @key
                return document(attend.meet)
        """, mapOf(
            "key" to key
        )
    )

    fun places(meet: String) = list(
        Place::class, """
            for place in @@collection
                filter place.meet == @meet
                return place
        """, mapOf(
            "meet" to meet
        )
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

    fun vote(attend: String, meet: String, place: String) = one(
        Vote::class, """
            upsert { attend: @attend, meet: @meet }
                insert { attend: @attend, meet: @meet, place: @place, createdAt: date_iso8601(date_now()) }
                update { place: @place }
                in @@collection
                return new
        """, mapOf(
            "attend" to attend,
            "meet" to meet,
            "place" to place
        )
    )

    fun confirm(attend: String, meet: String, place: String) = one(
        Confirm::class, """
            upsert { attend: @attend, meet: @meet }
                insert { attend: @attend, meet: @meet, place: @place, createdAt: date_iso8601(date_now()) }
                update { place: @place }
                in @@collection
                return new
        """, mapOf(
            "attend" to attend,
            "meet" to meet,
            "place" to place
        )
    )
}

private fun ArangoDatabase.setup() = apply {
    DbCollection.values().forEach { collection ->
        try {
            createCollection(collection.dbCollection())
        } catch (ignored: ArangoDBException) {
            // Most likely exists
        }
    }
}

private fun String.asKey() = this.split("/").last()

private fun <T : Model> KClass<T>.dbCollection() = DbCollection.values().first { this == it.model }.dbCollection()

private fun DbCollection.dbCollection() = name.lowercase()
