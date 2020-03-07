package logd

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.lang.Integer.min
import java.nio.file.Paths
import logd.cli.ServeCommand
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class App {
    private val log = LoggerFactory.getLogger(javaClass)

    lateinit var config: Config

    lateinit var db: DataSource

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
        val props = Properties()
        props["dataSourceClassName"] = "org.postgresql.ds.PGSimpleDataSource"
        props["dataSource.serverName"] = config.postgres.host
        props["dataSource.portNumber"] = config.postgres.port
        props["dataSource.databaseName"] = config.postgres.db
        props["dataSource.user"] = config.postgres.username
        props["dataSource.password"] = config.postgres.password
        val hikariConfig = HikariConfig(props)
        db = HikariDataSource(hikariConfig)
        log.info("Successfully connected to database.")
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
    CliApp(app)
        .subcommands(ServeCommand(app))
        .main(args)
}
