package logd.logging

import com.arangodb.ArangoDatabase
import com.arangodb.entity.BaseDocument
import com.arangodb.util.MapBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import logd.COLLECTION_NAME
import logd.Event
import logd.web.Jackson
import org.slf4j.LoggerFactory

class LoggingServiceImpl(val db: ArangoDatabase) : LoggingService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val tz = TimeZone.getDefault().toZoneId()

    override fun putEvents(events: List<Event>) {
        val collection = db.collection(COLLECTION_NAME)
        val docs = ArrayList<BaseDocument>(events.size)
        events.forEach {
            val doc = BaseDocument().apply {
                addAttribute("ts", it.ts.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                addAttribute("message", it.message)
                addAttribute("attrs", it.attrs)
            }
            log.trace("Document: {}", doc)
            docs.add(doc)
        }
        val result = collection.insertDocuments(docs)
        result.errors.forEach { log.error("Error: {}", it.errorMessage) }
        log.debug("Created document with key '{}'", result)
    }

    private val query = """
        FOR x IN $COLLECTION_NAME
        FILTER DATE_TIMESTAMP(x.ts) >= DATE_TIMESTAMP(@from)
           AND DATE_TIMESTAMP(x.ts) <= DATE_TIMESTAMP(@until)
        LIMIT 100
        RETURN x
    """.trimIndent()

    private val queryWithText = """
        FOR x IN $COLLECTION_NAME
        FILTER DATE_TIMESTAMP(x.ts) >= DATE_TIMESTAMP(@from)
           AND DATE_TIMESTAMP(x.ts) <= DATE_TIMESTAMP(@until)
           AND x.message LIKE @text
        LIMIT 100
        RETURN x 
    """.trimIndent()

    override fun searchEvents(from: String, until: String?, text: String?): List<Event> {
        val untilDT = until ?: ZonedDateTime
            .now(tz)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val bindVars = MapBuilder()
            .put("from", from)
            .put("until", untilDT)
        var queryText = query
        text?.let {
            bindVars.put("text", it)
            queryText = queryWithText
        }
        val result = db.query(queryText, bindVars.get(), String::class.java)
        val events = ArrayList<Event>()
        result.forEach { rawEvent ->
            events.add(Jackson.mapper.readValue(rawEvent))
        }
        return events
    }
}
