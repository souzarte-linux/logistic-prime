package com.fernando.centraldomotorista.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

object OutboxPayloadSerializer {

    val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    fun serialize(jsonObject: JsonObject): String = gson.toJson(jsonObject)

    fun deserialize(json: String): JsonObject = gson.fromJson(json, JsonObject::class.java)
}
