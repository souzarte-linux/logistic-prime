package com.fernando.centraldomotorista.util

/**
 * Modelo de dados para armazenar informações extraídas do parsing de etiquetas brasileiras.
 */
data class ParsedAddress(
    val recipientName: String? = null,
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val cep: String? = null,
    val fullFormattedAddress: String
)

/**
 * Utilitário para extração estruturada de informações de etiquetas de frete brasileiras
 * (Mercado Livre, Shopee, Amazon, Correios, Jadlog, etc.) via OCR do Google ML Kit.
 */
object BrazilianLabelParser {

    // Regex com priorização para identificação precisa de CEP brasileiro
    private val EXPLICIT_CEP_REGEX = Regex("""(?i)\bCEP[:\s]*(\d{5})[-.\s]?(\d{3})\b""")
    private val HYPHEN_CEP_REGEX = Regex("""\b(\d{5})-(\d{3})\b""")
    private val GENERIC_CEP_REGEX = Regex("""\b(\d{5})(\d{3})\b""")

    // Prefixos típicos de identificação de destinatário em etiquetas
    private val RECIPIENT_REGEX = Regex(
        """(?i)(?:destinat[aá]rio|recebedor|cliente|entregar\s+para|nome)[:\s]*([A-Za-zÀ-ÿ\s.'-]{3,45})"""
    )

    // Prefixos comuns de logradouro no Brasil
    private val STREET_PREFIX_REGEX = Regex(
        """(?i)\b(Rua|R\.|Avenida|Av\.|Alameda|Al\.|Travessa|Trav\.|Estrada|Estr\.|Rodovia|Rod\.|Praça|Pça\.|Viela|Passagem)\s+([A-Za-zÀ-ÿ0-9\s.,'-]+)"""
    )

    // Regex para captura de número predial
    private val NUMBER_REGEX = Regex(
        """(?i)(?:[,\sºnN°#]+|n[uú]mero[:\s]*|nº[:\s]*|num[:\s]*)(\d{1,6}|S\/N|SN)\b"""
    )

    // Regex para identificação de Bairro
    private val NEIGHBORHOOD_REGEX = Regex(
        """(?i)(?:bairro|b\.)[:\s]*([A-Za-zÀ-ÿ0-9\s'-]{2,30})"""
    )

    // Regex para formato "Cidade - UF" ou "Cidade/UF"
    private val CITY_STATE_REGEX = Regex(
        """(?i)\b([A-Za-zÀ-ÿ\s'-]{3,35})\s*[-/]\s*([A-Za-z]{2})\b"""
    )

    /**
     * Realiza o parsing de texto OCR bruto reconhecido de uma etiqueta física.
     */
    fun parse(rawText: String): ParsedAddress {
        if (rawText.isBlank()) {
            return ParsedAddress(fullFormattedAddress = "")
        }

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 1. Extração do CEP com priorização hierárquica
        val explicitMatch = EXPLICIT_CEP_REGEX.find(rawText)
        val rawCep = if (explicitMatch != null) {
            "${explicitMatch.groupValues[1]}-${explicitMatch.groupValues[2]}"
        } else {
            val hyphenMatch = HYPHEN_CEP_REGEX.find(rawText)
            if (hyphenMatch != null) {
                "${hyphenMatch.groupValues[1]}-${hyphenMatch.groupValues[2]}"
            } else {
                val genericMatch = GENERIC_CEP_REGEX.find(rawText)
                genericMatch?.let { "${it.groupValues[1]}-${it.groupValues[2]}" }
            }
        }

        // 2. Extração do Destinatário
        var recipientName: String? = null
        for (line in lines) {
            val recMatch = RECIPIENT_REGEX.find(line)
            if (recMatch != null) {
                val candidate = recMatch.groupValues[1].trim()
                if (isValidRecipient(candidate)) {
                    recipientName = candidate
                    break
                }
            }
        }

        // 3. Extração do Logradouro e Número
        var street: String? = null
        var number: String? = null

        for (line in lines) {
            val streetMatch = STREET_PREFIX_REGEX.find(line)
            if (streetMatch != null) {
                street = streetMatch.groupValues[1] + " " + streetMatch.groupValues[2].trim()

                // Remove possíveis pontuações finais do logradouro
                street = street.replace(Regex("""[;,.-]+$"""), "").trim()

                // Tenta extrair o número na mesma linha
                val afterStreet = line.substring(streetMatch.range.last.coerceAtMost(line.length - 1))
                val numMatch = NUMBER_REGEX.find(afterStreet) ?: NUMBER_REGEX.find(line)
                if (numMatch != null) {
                    val rawNum = numMatch.groupValues[1].trim()
                    if (rawNum.isNotBlank()) {
                        number = rawNum
                    }
                }
                break
            }
        }

        // 4. Extração de Bairro
        var neighborhood: String? = null
        for (line in lines) {
            val neighMatch = NEIGHBORHOOD_REGEX.find(line)
            if (neighMatch != null) {
                neighborhood = neighMatch.groupValues[1].trim()
                break
            }
        }

        // 5. Extração de Cidade e UF
        var city: String? = null
        var state: String? = null
        for (line in lines) {
            val cityStateMatch = CITY_STATE_REGEX.find(line)
            if (cityStateMatch != null) {
                val candidateCity = cityStateMatch.groupValues[1].trim()
                val candidateUf = cityStateMatch.groupValues[2].trim().uppercase()
                if (isKnownBrazilianState(candidateUf)) {
                    city = candidateCity
                    state = candidateUf
                    break
                }
            }
        }

        // 6. Montagem do endereço formatado para exibição e navegação veicular
        val fullFormatted = buildString {
            if (!street.isNullOrBlank()) {
                append(street)
                if (!number.isNullOrBlank()) {
                    append(", ").append(number)
                }
            }
            if (!neighborhood.isNullOrBlank()) {
                if (isNotEmpty()) append(" - ")
                append(neighborhood)
            }
            if (!city.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append(city)
                if (!state.isNullOrBlank()) {
                    append(" - ").append(state)
                }
            }
            if (!rawCep.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append("CEP ").append(rawCep)
            }
        }.ifBlank {
            // Se as heurísticas não detectaram campos individuais, utiliza as primeiras linhas limpas
            lines.take(3).joinToString(" ").take(140)
        }

        return ParsedAddress(
            recipientName = recipientName,
            street = street,
            number = number,
            neighborhood = neighborhood,
            city = city,
            state = state,
            cep = rawCep,
            fullFormattedAddress = fullFormatted
        )
    }

    private fun isValidRecipient(name: String): Boolean {
        if (name.length < 3) return false
        val lower = name.lowercase()
        // Evita falsos positivos como títulos de seções
        val blacklist = listOf("destinatario", "recebedor", "remetente", "origem", "declaracao", "codigo", "endereco")
        return !blacklist.contains(lower)
    }

    private fun isKnownBrazilianState(uf: String): Boolean {
        val ufs = setOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        )
        return ufs.contains(uf)
    }
}
