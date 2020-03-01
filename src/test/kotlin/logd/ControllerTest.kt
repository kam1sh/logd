package logd

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime
import logd.logging.LoggingService
import logd.web.Jackson.auto
import logd.web.WebController
import org.http4k.core.*
import org.junit.Before
import org.junit.Test

class ControllerTest {
    lateinit var loggingService: LoggingService
    lateinit var events: List<Event>
    lateinit var handler: HttpHandler
    val lens = Body.auto<List<Event>>().toLens()

    @Before
    fun init() {
        loggingService = mockk()
        events = listOf(
            Event(
                ts = ZonedDateTime.now(),
                message = "Hello, logd!",
                attrs = mapOf(
                    "level" to "debug"
                )
            )
        )
        handler = WebController(loggingService).asHttpHandler()
    }

    @Test
    fun testControllerPutsValidEvents() {
        every { loggingService.putEvents(any()) } returns Unit
        val baseRequest = Request(Method.POST, "/events/json")
        val request = lens(events, baseRequest)
        val response = handler(request)
        assert(response.status == Status.CREATED)
        verify { loggingService.putEvents(any()) }
        confirmVerified(loggingService)
    }

    @Test
    fun testControllerSearchesEvents() {
        every {
            loggingService.searchEvents(
                from = "2020-02-23T14:31:10+03:00",
                until = any(),
                text = "%logd%"
            )
        } returns events
        val request = Request(Method.GET, "/events/search")
            .query("from", "2020-02-23T14:31:10+03:00")
            .query("text", "%logd%")
        val response = handler(request)
        assert(response.status == Status.OK)
        val data = lens(response)
        assert(!data.isEmpty())
        verify { loggingService.searchEvents(from = "2020-02-23T14:31:10+03:00", until = any(), text = "%logd%") }
    }
}
