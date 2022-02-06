package org.morefriends.models

import com.arangodb.entity.DocumentField
import com.google.gson.annotations.SerializedName
import java.time.Instant

open class Model {
    @DocumentField(DocumentField.Type.ID)
    @SerializedName(value = "id", alternate = ["_id"])
    var id: String? = null
    var createdAt: Instant? = null
}

open class Edge : Model() {
    @DocumentField(DocumentField.Type.FROM)
    @SerializedName(value = "from", alternate = ["_from"])
    var from: String? = null

    @DocumentField(DocumentField.Type.TO)
    @SerializedName(value = "to", alternate = ["_to"])
    var to: String? = null
}
