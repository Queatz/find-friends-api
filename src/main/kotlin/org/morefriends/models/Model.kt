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
