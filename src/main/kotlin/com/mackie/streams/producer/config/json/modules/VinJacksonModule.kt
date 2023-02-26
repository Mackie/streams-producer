package com.mackie.streams.producer.config.json.modules

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleDeserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.module.SimpleSerializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.mackie.streams.producer.domain.primitives.Vin

class VinJacksonModule : SimpleModule() {
    override fun getModuleName(): String = this.javaClass.simpleName

    override fun setupModule(context: SetupContext) {
        val serializers = SimpleSerializers()
        serializers.addSerializer(Vin::class.java, VinSerializer())
        context.addSerializers(serializers)

        val deserializers = SimpleDeserializers()
        deserializers.addDeserializer(Vin::class.java, VinDeserializer())
        context.addDeserializers(deserializers)
    }

    class VinSerializer : StdSerializer<Vin>(Vin::class.java) {
        override fun serialize(vin: Vin, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeString(vin.value)
        }
    }

    class VinDeserializer : StdDeserializer<Vin>(Vin::class.java) {
        override fun deserialize(parser: JsonParser, context: DeserializationContext): Vin {
            val node: JsonNode = parser.codec.readTree(parser)
            return Vin(node.textValue())
        }
    }
}