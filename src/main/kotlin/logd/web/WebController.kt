package logd.web

import logd.App
import logd.Event
import logd.web.Jackson.auto
import org.http4k.core.*
import org.http4k.filter.GzipCompressionMode
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.HashMap

class WebController(val app: App) {

    fun asHttpHandler() = ServerFilters.GZip(compressionMode = GzipCompressionMode.Streaming)
        .then(
            routes(
                "/events/json" bind Method.POST to ::newEventFromJSON,
                "/events/search" bind Method.GET to ::search
            )
        )

    val eventLens = Body.auto<Event>().toLens()
    private fun newEventFromJSON(request: Request): Response {
        val event = eventLens(request)
        println(event)
        return Response(Status.CREATED)
    }

    private fun search(request: Request): Response {
        val evt = Event(
            ts = ZonedDateTime.now(TimeZone.getDefault().toZoneId()),
            message = "It works!",
            attrs = HashMap()
        )
        return eventLens(evt, Response(Status.OK))
    }

}
