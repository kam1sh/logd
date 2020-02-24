package logd.web

import com.arangodb.entity.BaseDocument
import com.arangodb.util.MapBuilder
import com.arangodb.velocypack.VPackSlice
import com.fasterxml.jackson.module.kotlin.readValue
import logd.App
import logd.COLLECTION_NAME
import logd.Event
import logd.web.Jackson.auto
import org.http4k.core.*
import org.http4k.filter.GzipCompressionMode
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class WebController(val app: App) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun asHttpHandler() = ServerFilters.GZip(compressionMode = GzipCompressionMode.Streaming)
        .then(
            routes(
                "/events/json" bind Method.POST to ::newEventFromJSON,
                "/events/search" bind Method.GET to ::search
            )
        )

    private val eventLens = Body.auto<Event>().toLens()
    private fun newEventFromJSON(request: Request): Response {
        val event = eventLens(request)
        log.trace("Event timestamp: {}", event.ts)
        val collection = app.db.collection(COLLECTION_NAME)
        val document = BaseDocument().apply {
            addAttribute("ts", event.ts.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            addAttribute("message", event.message)
            addAttribute("attrs", event.attrs)
        }
        log.trace("Document: {}", document)
        val result = collection.insertDocument(document)
        log.debug("Created document with key '{}'", result.key)
        return Response(Status.CREATED)
    }

    private val query = """
        FOR x IN $COLLECTION_NAME
        FILTER DATE_TIMESTAMP(x.ts) >= DATE_TIMESTAMP(@from)
           AND DATE_TIMESTAMP(x.ts) <= DATE_TIMESTAMP(@until)
        RETURN x
    """.trimIndent()

    private val eventsLens = Body.auto<List<Event>>().toLens()
    private fun search(request: Request): Response {
        val searchFromRaw: String
        var searchUntilRaw: String?
        try {
            searchFromRaw = request.query("from")!!
            searchUntilRaw = request.query("until")
            ZonedDateTime.parse(searchFromRaw)
            if (searchUntilRaw != null)
                ZonedDateTime.parse(searchUntilRaw)
            else {
                searchUntilRaw = ZonedDateTime
                    .now(TimeZone.getDefault().toZoneId())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }
        } catch (exc: Exception) {
            log.debug("Failed to parse search request: ", exc)
            return Response(Status.BAD_REQUEST)
        }
        val bindVars = MapBuilder()
            .put("from", searchFromRaw)
            .put("until", searchUntilRaw)
            .get()
        val result = app.db.query(query, bindVars, VPackSlice::class.java)
        var events: List<Event> = ArrayList()
        result.forEach { vPackSlice ->
            events = events.plus(vPackSlice.toEvent())
        }
        return eventsLens(events, Response(Status.OK))
    }

    private fun VPackSlice.toEvent(): Event {
        val rawAttrs = get("attrs").toString()
        log.trace("raw attrs: {}", rawAttrs)
        val attrs = Jackson.mapper.readValue<Map<String, String>>(rawAttrs)
        return Event(
            ts = get("ts").asString.toZonedDateTime(),
            message = get("message").asString,
            attrs = attrs
        )
    }

    private fun String.toZonedDateTime() = ZonedDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
