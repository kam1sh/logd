package logd

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

data class PostgresSettings(
    var host: String,
    var port: Int,
    var db: String,
    var username: String,
    var password: String
)

data class ListenSettings(var port: Int)

data class Config(val postgres: PostgresSettings, val listen: ListenSettings)

fun File.toConfig(): Config {
    val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
    return this
        .bufferedReader()
        .use { mapper.readValue(it, Config::class.java) }
}
