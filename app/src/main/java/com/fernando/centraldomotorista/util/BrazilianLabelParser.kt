package com.fernando.centraldomotorista.util

/**
 * Modelo de dados para armazenar informações extraídas do parsing de etiquetas brasileiras.
 */
data class ParsedAddress(
    val recipientName: String? = null,
    val street: String? = null,
    val number: String? = null,
    val complement: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val cep: String? = null,
    val reference: String? = null,
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

    // Prefixos ampliados de identificação de destinatário em etiquetas físicas
    private val RECIPIENT_LABEL_PATTERN = """(?i)(?:destinat[aá]rio(?:\s*[/]\s*recebedor)?|dest\b\.?|recebedor\b\.?|rec\b\.?|cliente\b|nome(?:\s+do\s+cliente)?\b|consignat[aá]rio\b|comprador\b|entregar\s+(?:para|a)\b|a\/c\b|aos\s+cuidados\s+de\b|para\b)"""
    
    private val RECIPIENT_INLINE_REGEX = Regex(
        """$RECIPIENT_LABEL_PATTERN[:\s-]+(.+)"""
    )
    private val RECIPIENT_ISOLATED_LABEL_REGEX = Regex(
        """^$RECIPIENT_LABEL_PATTERN[:\s-]*$"""
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
        """(?i)(?:bairro|b\.)[:\s]*([A-Za-zÀ-ÿ0-9\s'-]{2,35})"""
    )

    // Regex para formato "Cidade - UF" ou "Cidade/UF"
    private val CITY_STATE_REGEX = Regex(
        """(?i)\b([A-Za-zÀ-ÿ\s'-]{3,35})\s*[-/]\s*([A-Za-z]{2})\b"""
    )

    // Regex para identificação de Ponto de Referência / Instruções de Entrega / Notas
    private val REFERENCE_REGEX = Regex(
        """(?i)(?:ref(?:er[eê]ncia)?|ponto\s+de\s+ref(?:er[eê]ncia)?|obs(?:erva[cç][aã]o)?|instru[cç][oõ]es?|hor[aá]rio|recado|aten[cç][aã]o)[:\s-]+(.+)"""
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

        // 2. Extração do Destinatário (inline e multilinha) com limpeza avançada
        var recipientName: String? = null
        for (i in lines.indices) {
            val line = lines[i]

            // Caso A: Rótulo e nome na mesma linha (ex: "Destinatário: Carlos Eduardo Silva")
            val inlineMatch = RECIPIENT_INLINE_REGEX.find(line)
            if (inlineMatch != null) {
                val candidate = cleanRecipientString(inlineMatch.groupValues[1])
                if (isValidRecipient(candidate)) {
                    recipientName = candidate
                    break
                }
            }

            // Caso B: Rótulo multilinha isolado (ex: "DESTINATÁRIO:" e o nome na linha seguinte)
            if (RECIPIENT_ISOLATED_LABEL_REGEX.matches(line) && i + 1 < lines.size) {
                val nextLineCandidate = cleanRecipientString(lines[i + 1])
                if (isValidRecipient(nextLineCandidate)) {
                    recipientName = nextLineCandidate
                    break
                }
            }
        }

        // 3. Extração do Logradouro, Número e Complemento
        var street: String? = null
        var number: String? = null
        var complement: String? = null
        var streetLineIndex = -1

        for (i in lines.indices) {
            val line = lines[i]
            val streetMatch = STREET_PREFIX_REGEX.find(line)
            if (streetMatch != null) {
                streetLineIndex = i
                street = streetMatch.groupValues[1] + " " + streetMatch.groupValues[2].trim()

                // Tenta extrair complemento predial da linha do logradouro
                complement = AddressFormatter.extractComplement(line)

                // Remove complementos do nome da rua se detectados
                if (complement != null) {
                    street = street.replace(complement, "").trim()
                }

                // Remove possíveis pontuações finais do logradouro
                street = street.replace(Regex("""[;,.-]+$"""), "").trim()

                // Tenta extrair o número na mesma linha
                val afterStreet = line.substring(streetMatch.range.last.coerceAtMost(line.length - 1))
                val numMatch = NUMBER_REGEX.find(afterStreet) ?: NUMBER_REGEX.find(line)
                if (numMatch != null) {
                    val rawNum = numMatch.groupValues[1].trim()
                    if (rawNum.isNotBlank()) {
                        number = rawNum
                        // Limpa o número do logradouro se tiver sobrado
                        street = street.replace(Regex("""[,\s]+$rawNum\b"""), "").trim()
                    }
                }
                break
            }
        }

        // Heurística pré-endereço: se não encontrou destinatário por rótulo explícito,
        // avalia a linha anterior ao logradouro (de 2 a 5 palavras)
        if (recipientName == null && streetLineIndex > 0) {
            val candidateLine = cleanRecipientString(lines[streetLineIndex - 1])
            val words = candidateLine.split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (words.size in 2..5 && !candidateLine.contains(Regex("""\d""")) && isValidRecipient(candidateLine)) {
                recipientName = candidateLine
            }
        }

        // 4. Extração de Bairro
        var neighborhood: String? = null
        for (line in lines) {
            val neighMatch = NEIGHBORHOOD_REGEX.find(line)
            if (neighMatch != null) {
                neighborhood = neighMatch.groupValues[1].trim().replace(Regex("""[;,.-]+$"""), "")
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

        // 6. Extração de Referência / Notas / Observações
        var reference: String? = null
        for (line in lines) {
            val refMatch = REFERENCE_REGEX.find(line)
            if (refMatch != null) {
                reference = refMatch.groupValues[1].trim()
                break
            }
        }

        // Se houver complemento mas não foi detectado na linha da rua, busca em outras linhas
        if (complement == null) {
            for (line in lines) {
                if (line != street && !line.contains(rawCep ?: "###")) {
                    val comp = AddressFormatter.extractComplement(line)
                    if (comp != null && comp != number) {
                        complement = comp
                        break
                    }
                }
            }
        }

        // 7. Montagem do endereço formatado com Title Case e padrão oficial
        val fullFormatted = if (!street.isNullOrBlank() || !rawCep.isNullOrBlank()) {
            AddressFormatter.formatFullAddress(
                street = street,
                number = number,
                complement = complement,
                neighborhood = neighborhood,
                city = city,
                state = state,
                cep = rawCep
            )
        } else {
            // Se as heurísticas não detectaram campos individuais, utiliza as primeiras linhas limpas
            lines.take(3).joinToString(" ").take(140)
        }

        return ParsedAddress(
            recipientName = recipientName?.let { AddressFormatter.toTitleCase(it) },
            street = street?.let { AddressFormatter.toTitleCase(it) },
            number = number,
            complement = complement?.let { AddressFormatter.toTitleCase(it) },
            neighborhood = neighborhood?.let { AddressFormatter.toTitleCase(it) },
            city = city?.let { AddressFormatter.toTitleCase(it) },
            state = state?.uppercase(),
            cep = rawCep,
            reference = reference,
            fullFormattedAddress = fullFormatted
        )
    }

    /**
     * Remove sufixos como telefone, CPF, documentos fiscais que frequentemente
     * aparecem na mesma linha do nome do destinatário.
     */
    private fun cleanRecipientString(raw: String): String {
        var cleaned = raw.trim()
        // Remove trecho de CPF / Doc / Tel / Contato após o nome
        cleaned = cleaned.replace(Regex("""(?i)\s*[-/|]?\s*(?:cpf|tel|cel|telefone|doc|documento|rg|contato)\b.*"""), "")
        // Remove telefones embutidos (ex: (11) 98765-4321 ou 11987654321)
        cleaned = cleaned.replace(Regex("""(?:\(?\d{2}\)?\s*)?\d{4,5}-?\d{4}"""), "")
        // Remove CPF (ex: 123.456.789-00 ou 12345678900)
        cleaned = cleaned.replace(Regex("""\d{3}\.?\d{3}\.?\d{3}-?\d{2}"""), "")
        // Remove pontuações residuais no início e fim
        cleaned = cleaned.replace(Regex("""^[\s:;,-]+|[\s:;,-]+$"""), "")
        return cleaned.trim()
    }

    private fun isValidRecipient(name: String): Boolean {
        if (name.length < 3) return false
        val lower = name.lowercase().trim()
        val blacklist = listOf(
            "destinatario", "destinatário", "recebedor", "remetente", "origem",
            "declaracao", "declaração", "codigo", "código", "endereco", "endereço",
            "mercado livre", "mercado envios", "shopee", "shopee xpress", "amazon",
            "correios", "sedex", "pac", "jadlog", "logistica", "logística", "danfe",
            "nota fiscal", "nf-e", "chave de acesso", "conteudo", "conteúdo", "pacote",
            "transportadora", "peso", "volumes", "declarado", "valor", "remessa"
        )
        return !blacklist.any { lower.contains(it) }
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
