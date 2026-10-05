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
}
