package com.fernando.centraldomotorista.util

import java.util.Locale

/**
 * Utilitário de formatação de endereços em Title Case com tratamento rigoroso
 * para preposições brasileiras, UFs, siglas prediais e observações.
 */
object AddressFormatter {

    // Preposições, artigos e conjunções que devem permanecer em minúsculas
    // (a menos que sejam a primeira palavra da frase ou do segmento)
    private val LOWERCASE_WORDS = setOf(
        "de", "da", "do", "das", "dos",
        "e", "em", "no", "na", "nos", "nas",
        "com", "por", "para", "pra", "pro", "pras", "pros",
        "a", "o", "as", "os", "ao", "aos", "à", "às",
        "dum", "duma", "duns", "dumas"
    )

    // UFs brasileiras que devem sempre permanecer em MAIÚSCULAS
    private val BRAZILIAN_UFS = setOf(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
        "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
        "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    )

    // Siglas comuns que devem permanecer em maiúsculas
    private val ACRONYMS = setOf(
        "CEP", "S/N", "SN", "BR", "PF", "PJ"
    )

    /**
     * Converte um texto qualquer para Title Case aplicando as regras gramaticais da língua portuguesa:
     * - Palavras normais: Primeira letra maiúscula, restante minúscula.
     * - Preposições/artigos (de, da, do, das, dos, e, em, etc.): Minúsculas.
     * - Siglas e UFs (SP, RJ, CEP, S/N): Maiúsculas.
     */
    fun toTitleCase(input: String?): String {
        if (input.isNullOrBlank()) return ""

        val raw = input.trim()
        val tokens = raw.split(Regex("""(?<=\s)|(?=\s)|(?<=[-/,])|(?=[-/,])"""))

        val result = StringBuilder()
        var isFirstWordInSegment = true

        for (token in tokens) {
            when {
                token.isBlank() -> {
                    result.append(token)
                }
                token in listOf("-", "/", ",") -> {
                    result.append(token)
                    isFirstWordInSegment = true
                }
                else -> {
                    val cleanUpper = token.uppercase(Locale.ROOT)
                    val cleanLower = token.lowercase(Locale.ROOT)

                    val formatted = when {
                        // Verifica se é UF conhecida
                        BRAZILIAN_UFS.contains(cleanUpper) -> cleanUpper

                        // Verifica se é sigla conhecida
                        ACRONYMS.contains(cleanUpper) -> cleanUpper

                        // Se for preposição/artigo e não for a primeira palavra do segmento
                        !isFirstWordInSegment && LOWERCASE_WORDS.contains(cleanLower) -> cleanLower

                        // Palavra padrão: capitaliza primeira letra
                        else -> capitalizeWord(cleanLower)
                    }

                    result.append(formatted)
                    isFirstWordInSegment = false
                }
            }
        }

        return result.toString().trim()
    }

    private fun capitalizeWord(word: String): String {
        if (word.isEmpty()) return word
        return word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    /**
     * Formatação obrigatória:
     * "Nome da rua, número (etiqueta), complemento (apto/bloco/torre/prédio), Bairro, Cidade, Estado, CEP"
     */
    fun formatFullAddress(
        street: String?,
        number: String?,
        complement: String? = null,
        neighborhood: String?,
        city: String?,
        state: String?,
        cep: String?
    ): String {
        val parts = mutableListOf<String>()

        val cleanStreet = street?.trim()?.takeIf { it.isNotEmpty() }?.let { toTitleCase(it) }
        val cleanNumber = number?.trim()?.takeIf { it.isNotEmpty() }?.let {
            if (it.equals("sn", ignoreCase = true) || it.equals("s/n", ignoreCase = true)) "S/N" else it
        }

        if (cleanStreet != null) {
            if (cleanNumber != null) {
                parts.add("$cleanStreet, $cleanNumber")
            } else {
                parts.add(cleanStreet)
            }
        } else if (cleanNumber != null) {
            parts.add("Nº $cleanNumber")
        }

        val cleanComplement = complement?.trim()?.takeIf { it.isNotEmpty() }?.let { toTitleCase(it) }
        if (cleanComplement != null) {
            parts.add(cleanComplement)
        }

        val cleanNeighborhood = neighborhood?.trim()?.takeIf { it.isNotEmpty() }?.let { toTitleCase(it) }
        if (cleanNeighborhood != null) {
            parts.add(cleanNeighborhood)
        }

        val cleanCity = city?.trim()?.takeIf { it.isNotEmpty() }?.let { toTitleCase(it) }
        val cleanState = state?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(Locale.ROOT)

        if (cleanCity != null && cleanState != null) {
            parts.add("$cleanCity - $cleanState")
        } else if (cleanCity != null) {
            parts.add(cleanCity)
        } else if (cleanState != null) {
            parts.add(cleanState)
        }

        val cleanCep = cep?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanCep != null) {
            val formattedCep = if (cleanCep.startsWith("CEP", ignoreCase = true)) {
                cleanCep.uppercase(Locale.ROOT)
            } else {
                "CEP $cleanCep"
            }
            parts.add(formattedCep)
        }

        return parts.joinToString(", ")
    }

    /**
     * Extrai termos de complemento predial (ex: Apto 12, Bloco C, Torre 1) de um texto.
     */
    fun extractComplement(text: String): String? {
        val complementRegex = Regex(
            """(?i)\b((?:apto|apt|apartamento|bloco|bl|torre|tr|casa|cs|sala|sl|quadra|qd|lote|lt|fundos|frente|and|andar|sobrado)\s*[\w\dºª.-]+(?:\s*[\w\dºª.-]+)?)\b"""
        )
        return complementRegex.find(text)?.groupValues?.get(1)?.trim()
    }

    data class ExtractedAddressDetails(
        val number: String? = null,
        val complementAndReferences: String? = null
    )

    /**
     * Extrai número predial, complementos (apto, bloco) e pontos de referência de um endereço existente.
     */
    fun extractDetailsFromAddress(address: String?): ExtractedAddressDetails {
        if (address.isNullOrBlank()) return ExtractedAddressDetails()

        val raw = address.trim()
        val parts = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        var detectedNumber: String? = null
        val complementsAndRefs = mutableListOf<String>()

        val numberRegex = Regex("""^(?:n[ºo]?\s*)?(\d+[a-zA-Z]?|s/n|sn)$""", RegexOption.IGNORE_CASE)
        val cepRegex = Regex("""(?i)\bcep\b|\b\d{5}-?\d{3}\b""")
        val cityStateRegex = Regex("""(?i)\s*-\s*[a-z]{2}$""")

        if (parts.size >= 2) {
            // Identifica posições de CEP e Cidade-UF se existirem
            val cepIndex = parts.indexOfLast { cepRegex.containsMatchIn(it) }
            val cityStateIndex = parts.indexOfLast { cityStateRegex.containsMatchIn(it) }
            val neighborhoodIndex = if (cityStateIndex > 1) cityStateIndex - 1 else -1

            for (i in 1 until parts.size) {
                if (i == cepIndex) continue
                if (i == cityStateIndex) continue
                if (i == neighborhoodIndex && !hasComplementOrReferenceKeywords(parts[i])) continue

                val part = parts[i]

                // Número residencial puro
                if (detectedNumber == null && numberRegex.matches(part)) {
                    val match = numberRegex.find(part)
                    detectedNumber = match?.groupValues?.get(1)
                    continue
                }

                // Número com complemento inline (ex: "120 Apto 42")
                val inlineNumMatch = Regex("""^(?:n[ºo]?\s*)?(\d+[a-zA-Z]?|s/n|sn)\s+(.+)$""", RegexOption.IGNORE_CASE).find(part)
                if (detectedNumber == null && inlineNumMatch != null) {
                    detectedNumber = inlineNumMatch.groupValues[1]
                    val rem = inlineNumMatch.groupValues[2].trim()
                    if (rem.isNotEmpty()) {
                        complementsAndRefs.add(rem)
                    }
                    continue
                }

                complementsAndRefs.add(part)
            }

            // Se ainda não encontrou número, verifica se estava colado ao final da 1ª parte
            if (detectedNumber == null) {
                val firstPart = parts[0]
                val endNumMatch = Regex("""\s+(?:n[ºo]?\s*)?(\d+[a-zA-Z]?|s/n|sn)$""", RegexOption.IGNORE_CASE).find(firstPart)
                if (endNumMatch != null) {
                    detectedNumber = endNumMatch.groupValues[1]
                }
            }
        } else {
            // Endereço sem vírgula
            val numMatch = Regex("""\b(?:n[ºo]?\s*)?(\d+[a-zA-Z]?|s/n|sn)\b""", RegexOption.IGNORE_CASE).find(raw)
            if (numMatch != null) {
                detectedNumber = numMatch.groupValues[1]
            }

            val comp = extractComplement(raw)
            if (comp != null) {
                complementsAndRefs.add(comp)
            }
        }

        val cleanNumber = detectedNumber?.let {
            if (it.equals("sn", ignoreCase = true) || it.equals("s/n", ignoreCase = true)) "S/N" else it
        }

        val joinedComplements = complementsAndRefs
            .map { toTitleCase(it) }
            .distinct()
            .joinToString(", ")
            .takeIf { it.isNotBlank() }

        return ExtractedAddressDetails(
            number = cleanNumber,
            complementAndReferences = joinedComplements
        )
    }

    private fun hasComplementOrReferenceKeywords(text: String): Boolean {
        val keywordsRegex = Regex(
            """(?i)\b(apto|apt|bloco|bl|torre|tr|casa|cs|sala|sl|quadra|qd|lote|lt|fundos|frente|and|andar|sobrado|galp[aã]o|km|pr[oó]ximo|perto|em frente|ao lado|ref|obs)\b"""
        )
        return keywordsRegex.containsMatchIn(text)
    }

    /**
     * Mescla os dados de logradouro oficial retornados pelo ViaCEP com dados pré-existentes
     * em um endereço (como número residencial, complementos prediais e pontos de referência),
     * preservando integralmente tais informações.
     */
    fun mergeAddressPreservingDetails(
        previousAddress: String?,
        officialStreet: String?,
        officialNeighborhood: String?,
        officialCity: String?,
        officialState: String?,
        cep: String?,
        viaCepComplement: String? = null
    ): String {
        val extracted = extractDetailsFromAddress(previousAddress)

        val combinedComplement = when {
            extracted.complementAndReferences.isNullOrBlank() -> viaCepComplement?.takeIf { it.isNotBlank() }
            viaCepComplement.isNullOrBlank() -> extracted.complementAndReferences
            extracted.complementAndReferences.contains(viaCepComplement, ignoreCase = true) -> extracted.complementAndReferences
            else -> "${extracted.complementAndReferences}, $viaCepComplement"
        }

        return formatFullAddress(
            street = officialStreet,
            number = extracted.number,
            complement = combinedComplement,
            neighborhood = officialNeighborhood,
            city = officialCity,
            state = officialState,
            cep = cep
        )
    }
}
