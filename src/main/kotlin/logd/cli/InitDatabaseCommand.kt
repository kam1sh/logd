package logd.cli

import com.arangodb.ArangoDBException
import com.github.ajalt.clikt.core.CliktCommand
import logd.App
import logd.COLLECTION_NAME
import org.slf4j.LoggerFactory

class InitDatabaseCommand(val app: App) : CliktCommand(name = "initdb") {
    val log = LoggerFactory.getLogger(javaClass)

    override fun run() {
        try {
            val collection = app.db.createCollection(COLLECTION_NAME)
            val col = app.db.collection(COLLECTION_NAME)
            col.ensureFulltextIndex(listOf("message"), null)
            col.ensurePersistentIndex(listOf("ts"), null)
            log.info("Created collection logs ({})", collection)
        } catch (exc: ArangoDBException) {
            if (exc.errorNum == 1207) {
                log.error("Collection exists.")
                log.info("Exception: ", exc)
            } else log.error("Exception: ", exc)
        }
    }
}
