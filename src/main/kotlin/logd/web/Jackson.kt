package logd.web

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object Jackson : ConfigurableJackson(
    KotlinModule()
        .asConfigurable()
        .withStandardMappings()
        .done()
        .deactivateDefaultTyping()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
) {
    init {
        val jtModule = JavaTimeModule()
        val deserializer = ZonedDateTimeSerializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        jtModule.addSerializer(ZonedDateTime::class.java, deserializer)
        this.mapper.registerModule(jtModule)
    }
}
