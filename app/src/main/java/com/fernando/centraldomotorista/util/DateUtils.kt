package com.fernando.centraldomotorista.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Utilitário para garantir que datas recebidas de APIs remotas (como PostgREST / Supabase / Neon,
 * que serializam em UTC / "+00:00" / "Z") sejam convertidas para o fuso horário local do dispositivo
 * (ZoneId.systemDefault()).
 */
fun parseToLocalOffsetDateTime(dateStr: String?): OffsetDateTime? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val sanitized = dateStr.replace(" ", "T")
        OffsetDateTime.parse(sanitized).atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime()
    } catch (e: Exception) {
        try {
            Instant.parse(dateStr.replace(" ", "T"))
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
        } catch (e2: Exception) {
            null
        }
    }
}

/**
 * Converte um OffsetDateTime para o fuso horário local do dispositivo.
 */
fun OffsetDateTime.toLocalZone(): OffsetDateTime {
    return this.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime()
}

/**
 * Converte um OffsetDateTime nulo ou não-nulo para o fuso horário local do dispositivo.
 */
fun OffsetDateTime?.toLocalZoneOrNull(): OffsetDateTime? {
    return this?.atZoneSameInstant(ZoneId.systemDefault())?.toOffsetDateTime()
}
