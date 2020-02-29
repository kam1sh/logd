package logd.cli

import com.github.ajalt.clikt.core.CliktCommand
import logd.App
import logd.logging.LoggingServiceImpl
import logd.web.WebController
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.slf4j.LoggerFactory

class Serve(val app: App) : CliktCommand() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run() {
        val controller = WebController(LoggingServiceImpl(app.db))
        val server = controller.asHttpHandler().asServer(Undertow(app.config.listen.port))
        log.info("Starting server at port {}.", server.port())
        server.start().block()
    }
}
