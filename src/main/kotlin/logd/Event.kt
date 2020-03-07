package logd

import java.time.ZonedDateTime

const val COLLECTION_NAME = "events"

data class Event(
    var ts: ZonedDateTime,
    var message: String,
    var attrs: Map<String, String>
)
