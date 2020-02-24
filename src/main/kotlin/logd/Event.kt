package logd

import java.time.ZonedDateTime

data class Event(
    var ts: ZonedDateTime,
    var message: String,
    var attrs: Map<String, String>
)
