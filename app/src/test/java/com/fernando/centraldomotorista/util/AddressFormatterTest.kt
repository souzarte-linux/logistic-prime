package com.fernando.centraldomotorista.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressFormatterTest {

    @Test
    fun testToTitleCasePreservesPrepositionsAndConjunctions() {
        val input = "RUA DAS PALMEIRAS, 120"
        val formatted = AddressFormatter.toTitleCase(input)
        assertEquals("Rua das Palmeiras, 120", formatted)

        val input2 = "AVENIDA DO ESTADO DE SÃO PAULO"
        val formatted2 = AddressFormatter.toTitleCase(input2)
        assertEquals("Avenida do Estado de São Paulo", formatted2)

        val input3 = "TRAVESSA DOS PINHEIROS E FLORES"
        val formatted3 = AddressFormatter.toTitleCase(input3)
        assertEquals("Travessa dos Pinheiros e Flores", formatted3)
    }

    @Test
    fun testToTitleCasePreservesUfsAndAcronyms() {
        val input = "SÃO PAULO - SP"
        val formatted = AddressFormatter.toTitleCase(input)
        assertEquals("São Paulo - SP", formatted)

        val input2 = "RIO DE JANEIRO - RJ, CEP 20040-000"
        val formatted2 = AddressFormatter.toTitleCase(input2)
        assertEquals("Rio de Janeiro - RJ, CEP 20040-000", formatted2)

        val input3 = "ESTRADA DO CAMPO LIMPO, S/N"
        val formatted3 = AddressFormatter.toTitleCase(input3)
        assertEquals("Estrada do Campo Limpo, S/N", formatted3)
    }

    @Test
    fun testFormatFullAddressMandatoryOrder() {
        // Formatação obrigatória: Nome da rua, número (etiqueta), complemento (apto/bloco/torre/prédio), Bairro, Cidade, Estado, CEP
        val formatted = AddressFormatter.formatFullAddress(
            street = "Rua das Flores",
            number = "120",
            complement = "Apto 42",
            neighborhood = "Centro",
            city = "São Paulo",
            state = "SP",
            cep = "01001-000"
        )

        assertEquals("Rua das Flores, 120, Apto 42, Centro, São Paulo - SP, CEP 01001-000", formatted)
    }

    @Test
    fun testFormatFullAddressWithSnAndWithoutComplement() {
        val formatted = AddressFormatter.formatFullAddress(
            street = "ESTRADA DO CAMPO LIMPO",
            number = "SN",
            complement = null,
            neighborhood = "campo limpo",
            city = "são paulo",
            state = "sp",
            cep = "05787-000"
        )

        assertEquals("Estrada do Campo Limpo, S/N, Campo Limpo, São Paulo - SP, CEP 05787-000", formatted)
    }

    @Test
    fun testExtractComplement() {
        val compApto = AddressFormatter.extractComplement("Rua Augusta, 450, Apto 12B")
        assertEquals("Apto 12B", compApto)

        val compBloco = AddressFormatter.extractComplement("Av Paulista, 1000 - Bloco C Torre 2")
        assertTrue(compBloco?.contains("Bloco C") == true)

        val compCasa = AddressFormatter.extractComplement("Rua das Acácias, 88 Casa 2")
        assertEquals("Casa 2", compCasa)
    }

    @Test
    fun testMergeAddressPreservingDetailsWithNumberComplementAndReference() {
        val previousAddress = "Rua das Flores, 123, Apt 101, próximo do mercado de Senhor Luis, Jardim Primavera, São Paulo - SP, CEP 01310-100"

        val merged = AddressFormatter.mergeAddressPreservingDetails(
            previousAddress = previousAddress,
            officialStreet = "Rua Barão de Jaguara",
            officialNeighborhood = "Centro",
            officialCity = "Campinas",
            officialState = "SP",
            cep = "13010-000"
        )

        assertEquals(
            "Rua Barão de Jaguara, 123, Apt 101, Próximo do Mercado de Senhor Luis, Centro, Campinas - SP, CEP 13010-000",
            merged
        )
    }

    @Test
    fun testMergeAddressPreservingSnAndGalpao() {
        val previousAddress = "Estrada Velha, SN, Galpão 4, fundos"

        val merged = AddressFormatter.mergeAddressPreservingDetails(
            previousAddress = previousAddress,
            officialStreet = "Estrada do Campo Limpo",
            officialNeighborhood = "Campo Limpo",
            officialCity = "São Paulo",
            officialState = "SP",
            cep = "05787-000"
        )

        assertEquals(
            "Estrada do Campo Limpo, S/N, Galpão 4, Fundos, Campo Limpo, São Paulo - SP, CEP 05787-000",
            merged
        )
    }

    @Test
    fun testMergeAddressPreservingInlineNumberWithoutPreviousComplements() {
        val previousAddress = "Avenida Brasil, 450"

        val merged = AddressFormatter.mergeAddressPreservingDetails(
            previousAddress = previousAddress,
            officialStreet = "Avenida Paulista",
            officialNeighborhood = "Bela Vista",
            officialCity = "São Paulo",
            officialState = "SP",
            cep = "01311-000"
        )

        assertEquals(
            "Avenida Paulista, 450, Bela Vista, São Paulo - SP, CEP 01311-000",
            merged
        )
    }
}
