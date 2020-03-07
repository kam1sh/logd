package logd.logging

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import logd.COLLECTION_NAME
import logd.Event
import java.sql.Timestamp
import java.time.ZoneId
import javax.sql.DataSource
import kotlin.collections.ArrayList

class LoggingServiceImpl(val db: DataSource) : LoggingService {
    private val insertQuery = """
        INSERT INTO $COLLECTION_NAME (ts, message, attrs)
        VALUES (?, ?, ?)
    """.trimIndent()

    override fun putEvents(events: List<Event>) {
        db.connection.use {
            it.autoCommit = false
            val statement = it.prepareStatement(insertQuery)
            events.forEach {
                statement.apply {
                    setTimestamp(1, Timestamp.from(it.ts.toInstant()))
                    setString(2, it.message)
                    setObject(3, it.attrs)
                    execute()
                    close()
                }
            }
            it.commit()
        }
    }

    private val selectQuery = """
        SELECT ts, message, attrs from $COLLECTION_NAME
        WHERE ts BETWEEN ? AND ?
    """.trimIndent()

    private val selectQueryWithMessageFilter = """
        SELECT ts, message, attrs from $COLLECTION_NAME
        WHERE ts BETWEEN ? AND ?
          AND message ILIKE ?
    """.trimIndent()

    override fun searchEvents(from: String, until: String?, text: String?): List<Event> {
        // construct datetimes
        val untilDT = if (until != null)
            until.toZonedDateTime()
        else ZonedDateTime.now()
        val fromDT = from.toZonedDateTime()
        // select query text
        val queryText = if (text != null) selectQueryWithMessageFilter else selectQuery
        val events = ArrayList<Event>()
        db.connection.use {
            it.prepareStatement(queryText).use {
                it.apply {
                    setTimestamp(1, fromDT.toTimestamp())
                    setTimestamp(2, untilDT.toTimestamp())
                    text?.let {
                        setString(3, it)
                    }
                }
                val result = it.executeQuery()
                while (result.next()) {
                    val event = Event(
                        ts = result.getTimestamp(1).toZonedDateTime(),
                        message = result.getString(2),
                        attrs = result.getObject(3) as Map<String, String>
                    )
                    events.add(event)
                }
            }
        }
        return events
    }

    private fun String.toZonedDateTime() = ZonedDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    private fun ZonedDateTime.toTimestamp() = Timestamp.from(this.toInstant())
    private fun Timestamp.toZonedDateTime() = ZonedDateTime.ofInstant(toInstant(), ZoneId.of("UTC"))
}
