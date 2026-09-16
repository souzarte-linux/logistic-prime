package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.OilChange
import com.fernando.centraldomotorista.util.parseToLocalOffsetDateTime
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.OffsetDateTime

data class OilChangeDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("changed_at")
    val changedAt: String? = null,
    @SerializedName("km_at_change")
    val kmAtChange: BigDecimal,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun OilChangeDto.toDomain(): OilChange {
    val parsedChangedAt = parseToLocalOffsetDateTime(changedAt) ?: OffsetDateTime.now()

    return OilChange(
        id = id ?: "",
        userId = userId,
        changedAt = parsedChangedAt,
        kmAtChange = kmAtChange,
        notes = notes
    )
}

fun OilChange.toDto(): OilChangeDto {
    return OilChangeDto(
        id = id.ifBlank { null },
        userId = userId,
        changedAt = changedAt.toString(),
        kmAtChange = kmAtChange,
        notes = notes
    )
}
