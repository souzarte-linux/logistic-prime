package com.fernando.centraldomotorista.data.local.costcenter

import androidx.room.TypeConverter
import org.json.JSONArray
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

class CostCenterTypeConverters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? =
        value?.takeIf { it.isNotBlank() }?.let { raw ->
            try {
                BigDecimal(raw)
            } catch (_: NumberFormatException) {
                null
            }
        }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.takeIf { it.isNotBlank() }?.let { raw ->
            try {
                LocalDate.parse(raw)
            } catch (_: Exception) {
                null
            }
        }

    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? =
        value?.takeIf { it.isNotBlank() }?.let { raw ->
            try {
                OffsetDateTime.parse(raw).toInstant()
            } catch (_: Exception) {
                try {
                    Instant.parse(raw)
                } catch (_: Exception) {
                    null
                }
            }
        }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        val array = JSONArray()
        value?.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            List(array.length()) { index -> array.optString(index) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        val array = JSONArray()
        value?.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            List(array.length()) { index -> array.optInt(index) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
