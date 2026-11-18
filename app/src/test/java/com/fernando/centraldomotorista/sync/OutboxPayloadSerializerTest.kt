package com.fernando.centraldomotorista.sync

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxPayloadSerializerTest {

    @Test
    fun payloadComNullExplicitoEPreservado() {
        val jsonObject = JsonObject().apply {
            addProperty("name", "Seguro Auto")
            addProperty("category", "veiculo")
            add("deleted_at", JsonNull.INSTANCE)
            add("start_date", JsonNull.INSTANCE)
        }

        val serialized = OutboxPayloadSerializer.serialize(jsonObject)

        // Deve conter as chaves com valor null explícito
        assertTrue("JSON deve conter 'deleted_at': null", serialized.contains("\"deleted_at\":null") || serialized.contains("\"deleted_at\": null"))
        assertTrue("JSON deve conter 'start_date': null", serialized.contains("\"start_date\":null") || serialized.contains("\"start_date\": null"))
        assertFalse("JSON não deve omitir propriedades com valor null", !serialized.contains("deleted_at"))

        // Na desserialização, as chaves nulas continuam existindo
        val deserialized = OutboxPayloadSerializer.deserialize(serialized)
        assertTrue(deserialized.has("deleted_at"))
        assertTrue(deserialized.get("deleted_at").isJsonNull)
        assertTrue(deserialized.has("start_date"))
        assertTrue(deserialized.get("start_date").isJsonNull)
        assertEquals("Seguro Auto", deserialized.get("name").asString)
    }
}
