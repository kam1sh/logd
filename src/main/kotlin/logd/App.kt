package logd

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import com.arangodb.ArangoDB
import com.arangodb.ArangoDatabase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.lang.Integer.min
import java.nio.file.Paths
import logd.cli.InitDatabaseCommand
import logd.cli.ServeCommand
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class App {
    private val log = LoggerFactory.getLogger(javaClass)

    lateinit var config: Config

    lateinit var arango: ArangoDB
    lateinit var db: ArangoDatabase

    fun setup(configFile: File, logLevel: Level) {
        setupLogging(logLevel)
        config = configFile.toConfig()
        connectToDatabase()
    }

    fun setupLogging(level: Level = Level.INFO) {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.getLogger(Logger.ROOT_LOGGER_NAME).level = Level.WARN
        lc.getLogger("logd").level = level
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc)
        log.info("Using log level: {}", level)
    }

    fun connectToDatabase() {
        arango = ArangoDB.Builder()
            .host(config.arango.host, config.arango.port)
            .user(config.arango.username)
            .password(config.arango.password)
            .useSsl(config.arango.ssl)
            .build()
        db = arango.db(config.arango.db)
    }

    fun shutdown() {
        if (::arango.isInitialized) {
            log.info("Shutting down database connection.")
            arango.shutdown()
        }
    }
}

class CliApp(val app: App) : CliktCommand() {
    val config: File? by option(help = "Path to configuration file", envvar = "LOGD_CONFIG")
        .file(exists = true, fileOkay = true, folderOkay = false)
    val verbose: Int by option("-v", help = "Increase verbosity").counted()

    override fun run() {
        val levels = listOf(Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE)
        val level = levels[min(verbose, 3)]
        app.setup(configFile = config ?: Paths.get("logd.yml").toFile(), logLevel = level)
    }
}

fun main(args: Array<String>) {
    val app = App()
    try {
        CliApp(app)
            .subcommands(InitDatabaseCommand(app), ServeCommand(app))
            .main(args)
    } finally {
        app.shutdown()
    }
}
