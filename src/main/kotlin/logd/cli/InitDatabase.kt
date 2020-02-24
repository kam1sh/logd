package logd.cli

import com.arangodb.ArangoDBException
import com.github.ajalt.clikt.core.CliktCommand
import logd.App
import org.slf4j.LoggerFactory

class InitDatabase(val app: App) : CliktCommand(name = "initdb") {
    val log = LoggerFactory.getLogger(javaClass)
    override fun run() {
        try {
            val collection = app.db.createCollection("logs")
            log.info("Created collection logs ({})", collection)
        } catch (exc: ArangoDBException) {
            if (exc.errorNum == 1207) {
                log.error("Collection exists.")
                log.info("Exception: ", exc)
            } else log.error("Exception: ", exc)
        }
    }
}
