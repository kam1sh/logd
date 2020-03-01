package logd.logging

import logd.Event

interface LoggingService {
    fun putEvents(events: List<Event>)
    fun searchEvents(from: String, until: String? = null, text: String? = null): List<Event>
}
