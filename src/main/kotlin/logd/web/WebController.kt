package logd.web

import logd.Event
import logd.logging.LoggingService
import logd.web.Jackson.auto
import org.http4k.core.*
import org.http4k.filter.GzipCompressionMode
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.ZonedDateTime

class WebController(private val loggingService: LoggingService) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun asHttpHandler() = ServerFilters.GZip(compressionMode = GzipCompressionMode.Streaming)
        .then(
            routes(
                "/events/json" bind Method.POST to ::newEventFromJSON,
                "/events/search" bind Method.GET to ::search
            )
        )

    private val eventsLens = Body.auto<List<Event>>().toLens()
    private fun newEventFromJSON(request: Request): Response {
        val events = eventsLens(request)
        loggingService.putEvents(events)
        return Response(Status.CREATED)
    }

    private fun search(request: Request): Response {
        val searchFromRaw: String
        val searchUntilRaw: String?
        val text: String? = request.query("text")
        try {
            searchFromRaw = request.query("from")!!
            searchUntilRaw = request.query("until")
            // check that datetime format is valid
            ZonedDateTime.parse(searchFromRaw)
            if (searchUntilRaw != null)
                ZonedDateTime.parse(searchUntilRaw)
        } catch (exc: Exception) {
            log.debug("Failed to parse search request: ", exc)
            return Response(Status.BAD_REQUEST)
        }
        val events = loggingService.searchEvents(searchFromRaw, searchUntilRaw, text)
        return eventsLens(events, Response(Status.OK))
    }

}
