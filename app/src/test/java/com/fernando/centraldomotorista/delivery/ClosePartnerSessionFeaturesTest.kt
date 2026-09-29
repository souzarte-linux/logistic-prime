package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.ui.screens.deliverypartners.ScannedItemExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId

class ClosePartnerSessionFeaturesTest {

    @Test
    fun testStartDateAndHourModificationLogic() {
        val initialStartTime = OffsetDateTime.parse("2026-09-23T08:00:00-03:00")
        val currentZone = ZoneId.systemDefault()

        // Alterar Data
        val currentZoned = initialStartTime.atZoneSameInstant(currentZone)
        val afterDateChange = currentZoned
            .withYear(2026)
            .withMonth(9)
            .withDayOfMonth(25)
            .toOffsetDateTime()

        assertEquals(25, afterDateChange.atZoneSameInstant(currentZone).dayOfMonth)
        assertEquals(8, afterDateChange.atZoneSameInstant(currentZone).hour)

        // Alterar Hora
        val afterTimeChange = afterDateChange.atZoneSameInstant(currentZone)
            .withHour(10)
            .withMinute(45)
            .withSecond(0)
            .toOffsetDateTime()

        val finalZoned = afterTimeChange.atZoneSameInstant(currentZone)
        assertEquals(25, finalZoned.dayOfMonth)
        assertEquals(9, finalZoned.monthValue)
        assertEquals(2026, finalZoned.year)
        assertEquals(10, finalZoned.hour)
        assertEquals(45, finalZoned.minute)
    }

    @Test
    fun testScannedItemsSortedSuccessFirstThenFailure() {
        val expeditionBarcodes = listOf("PKG_A", "PKG_B", "PKG_C", "PKG_D", "PKG_E")
        val returnedBarcodes = setOf("PKG_B", "PKG_E")

        val allCodes = (expeditionBarcodes + returnedBarcodes).distinct()
        val sortedList = allCodes.sortedWith(
            compareBy<String> { code -> if (returnedBarcodes.contains(code)) 1 else 0 }
                .thenBy { it }
        )

        // Os pacotes com sucesso de entrega (não devolvidos) devem vir primeiro
        assertEquals(listOf("PKG_A", "PKG_C", "PKG_D", "PKG_B", "PKG_E"), sortedList)

        // Primeiros 3 são entregues (sucesso)
        assertFalse(returnedBarcodes.contains(sortedList[0]))
        assertFalse(returnedBarcodes.contains(sortedList[1]))
        assertFalse(returnedBarcodes.contains(sortedList[2]))

        // Últimos 2 são devolvidos (insucesso)
        assertTrue(returnedBarcodes.contains(sortedList[3]))
        assertTrue(returnedBarcodes.contains(sortedList[4]))
    }

    @Test
    fun testScannedItemExportMapping() {
        val allCodes = listOf("PKG001", "PKG002")
        val returnedBarcodes = setOf("PKG002")

        val exportItems = allCodes.map { code ->
            ScannedItemExport(
                barcode = code,
                isReturned = returnedBarcodes.contains(code)
            )
        }

        assertEquals(2, exportItems.size)
        assertEquals("PKG001", exportItems[0].barcode)
        assertFalse(exportItems[0].isReturned)

        assertEquals("PKG002", exportItems[1].barcode)
        assertTrue(exportItems[1].isReturned)
    }

    @Test
    fun testRemovingReturnedBarcodeRestoresSuccessStatus() {
        var returnedBarcodes = setOf("PKG001", "PKG002")
        val codeTarget = "PKG001"

        assertTrue(returnedBarcodes.contains(codeTarget))

        // Remover código da lista de devoluções
        returnedBarcodes = returnedBarcodes - codeTarget
        assertFalse(returnedBarcodes.contains(codeTarget))

        // No ordenamento, PKG001 volta a ser sucesso (entregue) e fica antes de PKG002
        val allCodes = listOf("PKG001", "PKG002")
        val sortedList = allCodes.sortedWith(
            compareBy<String> { code -> if (returnedBarcodes.contains(code)) 1 else 0 }
                .thenBy { it }
        )

        assertEquals("PKG001", sortedList[0])
        assertEquals("PKG002", sortedList[1])
    }

    @Test
    fun testClosePartnerSessionCompositionPropriosVsImportados() {
        val sessionPackages = listOf(
            com.fernando.centraldomotorista.data.model.PartnerSessionPackage(
                id = "p-1",
                sessionId = "sess-1",
                barcode = "BR123456789",
                origin = com.fernando.centraldomotorista.data.model.PackageOrigin.IMPORTADO_MASTER
            ),
            com.fernando.centraldomotorista.data.model.PartnerSessionPackage(
                id = "p-2",
                sessionId = "sess-1",
                barcode = "BR987654321",
                origin = com.fernando.centraldomotorista.data.model.PackageOrigin.NOVO
            )
        )

        val session = com.fernando.centraldomotorista.data.model.DeliveryPartnerSession(
            id = "sess-1",
            userId = "usr-1",
            partnerId = "partner-1",
            expectedPackageCount = 10,
            scannedCount = 10,
            packageRate = java.math.BigDecimal("4.50")
        )

        val uiState = com.fernando.centraldomotorista.ui.screens.deliverypartners.ClosePartnerSessionUiState(
            session = session,
            sessionPackages = sessionPackages
        )

        // 1 importado e 9 próprios (de 10 totais)
        assertEquals(1, uiState.importedMasterCount)
        assertEquals(9, uiState.ownPackagesCount)

        assertTrue(uiState.isImportedFromMaster("BR123456789"))
        assertFalse(uiState.isImportedFromMaster("BR987654321"))
        assertFalse(uiState.isImportedFromMaster("NON_EXISTING"))
    }
}
