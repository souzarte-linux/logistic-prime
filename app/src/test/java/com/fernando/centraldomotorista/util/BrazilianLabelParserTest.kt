package com.fernando.centraldomotorista.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o analisador de etiquetas de frete brasileiras (BrazilianLabelParser).
 */
class BrazilianLabelParserTest {

    @Test
    fun testParseMercadoLivreLabel() {
        val label = """
            MERCADO LIVRE - LOGÍSTICA
            PACOTE: 420918237BR
            Destinatário: Carlos Eduardo Silva
            Rua das Palmeiras, 120
            Bairro: Centro
            São Paulo - SP
            CEP: 01001-000
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(label)

        assertEquals("Carlos Eduardo Silva", parsed.recipientName)
        assertEquals("01001-000", parsed.cep)
        assertTrue("Logradouro deve conter Rua das Palmeiras", parsed.street?.contains("Rua das Palmeiras") == true)
        assertEquals("120", parsed.number)
        assertEquals("Centro", parsed.neighborhood)
        assertEquals("São Paulo", parsed.city)
        assertEquals("SP", parsed.state)
        assertTrue("Endereço formatado deve conter logradouro e CEP", parsed.fullFormattedAddress.contains("Rua das Palmeiras, 120"))
    }

    @Test
    fun testParseShopeeLabel() {
        val label = """
            SHOPEE XPRESS
            Recebedor: Mariana Santos
            Av. Paulista, 1000
            Bairro: Bela Vista
            São Paulo - SP
            01310-100
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(label)

        assertEquals("Mariana Santos", parsed.recipientName)
        assertEquals("01310-100", parsed.cep)
        assertTrue("Logradouro deve conter Av. Paulista", parsed.street?.contains("Av. Paulista") == true)
        assertEquals("1000", parsed.number)
        assertEquals("Bela Vista", parsed.neighborhood)
        assertEquals("São Paulo", parsed.city)
        assertEquals("SP", parsed.state)
    }

    @Test
    fun testParseLabelWithWithoutNumberSN() {
        val label = """
            CORREIOS SEDEX
            Cliente: João da Silva
            Estrada do Campo Limpo, S/N
            CEP 05787-000
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(label)

        assertEquals("João da Silva", parsed.recipientName)
        assertEquals("05787-000", parsed.cep)
        assertTrue("Deve conter Estrada do Campo Limpo", parsed.street?.contains("Estrada do Campo Limpo") == true)
        assertEquals("S/N", parsed.number)
    }

    @Test
    fun testParseEmptyAndBlankText() {
        val emptyResult = BrazilianLabelParser.parse("")
        assertNull(emptyResult.recipientName)
        assertNull(emptyResult.cep)
        assertEquals("", emptyResult.fullFormattedAddress)

        val blankResult = BrazilianLabelParser.parse("   \n\n  ")
        assertNull(blankResult.recipientName)
        assertNull(blankResult.cep)
        assertEquals("", blankResult.fullFormattedAddress)
    }

    @Test
    fun testParseCepWithoutHyphen() {
        val label = """
            Destinatário: Fernando Souza
            Rua Augusta, 450
            01305000
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(label)

        assertEquals("Fernando Souza", parsed.recipientName)
        assertEquals("01305-000", parsed.cep)
        assertTrue("Deve conter Rua Augusta", parsed.street?.contains("Rua Augusta") == true)
        assertEquals("450", parsed.number)
    }

    @Test
    fun testParseMultiLineRecipient() {
        val label = """
            MERCADO LIVRE
            DESTINATÁRIO:
            Lucas Martins
            Rua Vergueiro, 1500
            04101-000
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(label)
        assertEquals("Lucas Martins", parsed.recipientName)
        assertEquals("04101-000", parsed.cep)
        assertEquals("1500", parsed.number)
    }

    @Test
    fun testParseExtendedPrefixes() {
        val labelPara = """
            Para:
            Beatriz Lima
            Avenida Brasil, 200
            CEP 20040-000
        """.trimIndent()
        assertEquals("Beatriz Lima", BrazilianLabelParser.parse(labelPara).recipientName)

        val labelConsignatario = """
            Consignatário: Rodrigo Oliveira
            Rua das Acácias, 88
            13010-000
        """.trimIndent()
        assertEquals("Rodrigo Oliveira", BrazilianLabelParser.parse(labelConsignatario).recipientName)

        val labelComprador = """
            Comprador:
            Juliana Mendes
            Alameda Santos, 500
            01419-000
        """.trimIndent()
        assertEquals("Juliana Mendes", BrazilianLabelParser.parse(labelComprador).recipientName)
    }

    @Test
    fun testParsePreAddressHeuristic() {
        val labelWithoutLabel = """
            PACOTE LOGÍSTICA
            Carlos Eduardo Silva
            Rua das Palmeiras, 120
            Bairro Centro
            São Paulo - SP
            CEP: 01001-000
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(labelWithoutLabel)
        assertEquals("Carlos Eduardo Silva", parsed.recipientName)
        assertEquals("01001-000", parsed.cep)
        assertEquals("120", parsed.number)
    }

    @Test
    fun testParseShortDestAndStrippingPhoneAndCpf() {
        val label = """
            DEST: CARLOS EDUARDO SILVA - TEL: (11) 98765-4321
            Rua das Palmeiras, 120 - Apto 42
            Bairro: Centro
            São Paulo - SP
            CEP: 01001-000
            Ref: Deixar na portaria
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(label)
        assertEquals("Carlos Eduardo Silva", parsed.recipientName)
        assertEquals("01001-000", parsed.cep)
        assertEquals("120", parsed.number)
        assertEquals("Apto 42", parsed.complement)
        assertEquals("Deixar na portaria", parsed.reference)
    }

    @Test
    fun testParseNomeDoClienteWithCpf() {
        val label = """
            NOME DO CLIENTE: MARIA DE LOURDES SOUZA CPF: 123.456.789-00
            Avenida Paulista, 1000 Bloco B
            Bairro: Bela Vista
            São Paulo - SP
            01310-100
        """.trimIndent()

        val parsed = BrazilianLabelParser.parse(label)
        assertEquals("Maria de Lourdes Souza", parsed.recipientName)
        assertEquals("01310-100", parsed.cep)
        assertEquals("1000", parsed.number)
        assertEquals("Bloco B", parsed.complement)
    }
}
