package logd

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

data class ArangoSettings(
    var host: String,
    var port: Int,
    var db: String,
    var username: String,
    var password: String,
    var ssl: Boolean
)

data class ListenSettings(var port: Int)

data class Config(val arango: ArangoSettings, val listen: ListenSettings)

fun File.toConfig(): Config {
    val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
    return this
        .bufferedReader()
        .use { mapper.readValue(it, Config::class.java) }
}
