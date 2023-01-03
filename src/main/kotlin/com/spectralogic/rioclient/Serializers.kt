package com.spectralogic.rioclient

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.UUID

object HttpStatusCodeSerializer : KSerializer<HttpStatusCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HttpStatusCode", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): HttpStatusCode {
        return HttpStatusCode.fromValue(decoder.decodeInt())
    }

    override fun serialize(encoder: Encoder, value: HttpStatusCode) {
        encoder.encodeInt(value.value)
    }
}

object URISerializer : KSerializer<URI> {
    override val descriptor = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): URI {
        return URI(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }
}

/*object URLSerializer : KSerializer<URL> {
    override val descriptor = PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): URL {
        return URL(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: URL) {
        encoder.encodeString(value.toString())
    }
}*/

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}

object RioResourceErrorMessageSerializer : KSerializer<RioResourceErrorMessage> {
    override val descriptor = PrimitiveSerialDescriptor("RioResourceErrorMessage", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RioResourceErrorMessage {
        return Json.decodeFromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: RioResourceErrorMessage) {
        encoder.encodeString(Json.encodeToString(value))
    }
}

object RioValidationErrorMessageSerializer : KSerializer<RioValidationErrorMessage> {
    override val descriptor = PrimitiveSerialDescriptor("RioValidationErrorMessage", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RioValidationErrorMessage {
        return Json.decodeFromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: RioValidationErrorMessage) {
        encoder.encodeString(Json.encodeToString(value))
    }
}

object RioUnsupportedMediaErrorSerializer : KSerializer<RioUnsupportedMediaErrorMessage> {
    override val descriptor = PrimitiveSerialDescriptor("RioUnsupportedMediaError", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RioUnsupportedMediaErrorMessage {
        return Json.decodeFromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: RioUnsupportedMediaErrorMessage) {
        encoder.encodeString(Json.encodeToString(value))
    }
}