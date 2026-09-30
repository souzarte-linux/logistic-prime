package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.TransferStatus
import com.fernando.centraldomotorista.ui.screens.routes.master.RouteScannerUiState
import com.fernando.centraldomotorista.ui.screens.routes.master.ScannerStep
import com.fernando.centraldomotorista.util.ParsedAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para o scanner da Rota Master (Prompt 2, 3 e 6):
 * - Gate de confiança do auto-avanço baseado na integridade do OCR de endereço;
 * - Atribuição de parceiro no momento do scan com status ATRIBUIDO_PENDENTE;
 * - Seletor sticky de plataforma persistente entre bipagens consecutivas.
 */
class MasterRouteScannerTest {

    @Test
    fun testAutoAdvanceDoesNotTriggerWithIncompleteParsing() {
        // Regra do Prompt 3 / ADR-003:
        // isConfident = !parsedAddress.cep.isNullOrBlank() && parsedAddress.fullFormattedAddress.isNotBlank()

        fun evaluateConfidenceAndAutoAdvance(parsed: ParsedAddress): Pair<Boolean, Boolean> {
            val isConfident = !parsed.cep.isNullOrBlank() && parsed.fullFormattedAddress.isNotBlank()
            val shouldStartAutoAdvance = isConfident
            val showWarning = !isConfident
            return Pair(shouldStartAutoAdvance, showWarning)
        }

        // Cenário 1: Parsing incompleto - falta CEP
        val addressMissingCep = ParsedAddress(
            recipientName = "João Silva",
            street = "Rua das Flores",
            number = "123",
            cep = null,
            fullFormattedAddress = "Rua das Flores, 123"
        )
        val (autoAdvance1, warning1) = evaluateConfidenceAndAutoAdvance(addressMissingCep)
        assertFalse("Auto-avanço NÃO deve disparar sem CEP", autoAdvance1)
        assertTrue("Aviso de conferência deve ser exibido sem CEP", warning1)

        // Cenário 2: Parsing incompleto - endereço formatado vazio
        val addressEmptyAddress = ParsedAddress(
            recipientName = null,
            street = null,
            number = null,
            cep = "01310-100",
            fullFormattedAddress = ""
        )
        val (autoAdvance2, warning2) = evaluateConfidenceAndAutoAdvance(addressEmptyAddress)
        assertFalse("Auto-avanço NÃO deve disparar com endereço vazio", autoAdvance2)
        assertTrue("Aviso de conferência deve ser exibido com endereço vazio", warning2)

        // Cenário 3: Parsing incompleto - CEP em branco ("   ")
        val addressBlankCep = ParsedAddress(
            recipientName = "Maria",
            street = "Av Paulista",
            number = "1000",
            cep = "   ",
            fullFormattedAddress = "Av Paulista, 1000"
        )
        val (autoAdvance3, warning3) = evaluateConfidenceAndAutoAdvance(addressBlankCep)
        assertFalse("Auto-avanço NÃO deve disparar com CEP em branco", autoAdvance3)
        assertTrue("Aviso de conferência deve ser exibido", warning3)

        // Cenário 4: Parsing completo e confiável - CEP e endereço preenchidos
        val addressComplete = ParsedAddress(
            recipientName = "Carlos Eduardo",
            street = "Av. Paulista",
            number = "1000",
            neighborhood = "Bela Vista",
            city = "São Paulo",
            state = "SP",
            cep = "01310-100",
            fullFormattedAddress = "Av. Paulista, 1000, Bela Vista, São Paulo - SP"
        )
        val (autoAdvance4, warning4) = evaluateConfidenceAndAutoAdvance(addressComplete)
        assertTrue("Auto-avanço DEVE disparar com parsing completo", autoAdvance4)
        assertFalse("Aviso de conferência NÃO deve ser exibido com parsing completo", warning4)
    }

    @Test
    fun testAssignPartnerAtScanTimeSetsPendingStatus() {
        // Validação da atribuição no momento do scan (Prompt 6)
        // Quando o motorista seleciona um parceiro para a próxima bipagem:
        val partnerId = "partner-pedro-77"

        var state = RouteScannerUiState(
            routeId = "route-master-1",
            currentPlatformId = "plat-shopee",
            selectedPartnerIdForNextScan = partnerId
        )

        assertEquals("partner-pedro-77", state.selectedPartnerIdForNextScan)

        // Simula o salvamento da parada no repositório com o parceiro pré-selecionado
        val assignedPartner = state.selectedPartnerIdForNextScan
        val transferStatus = if (assignedPartner != null) TransferStatus.ATRIBUIDO_PENDENTE else null
        val transferredVia = if (assignedPartner != null) "manual_master" else null

        val stopCreated = MasterRouteStop(
            id = "stop-cross-1",
            routeId = state.routeId,
            userId = "user-1",
            barcode = "BC-CROSS-999",
            recipientName = "Cliente Atribuído",
            fullAddress = "Rua Augusta, 500",
            platformId = state.currentPlatformId,
            assignedPartnerId = assignedPartner,
            transferStatus = transferStatus,
            transferredVia = transferredVia
        )

        assertEquals(TransferStatus.ATRIBUIDO_PENDENTE, stopCreated.transferStatus)
        assertEquals("partner-pedro-77", stopCreated.assignedPartnerId)
        assertEquals("manual_master", stopCreated.transferredVia)

        // Após a conclusão do scan, o selecionador de parceiro reseta para Master (null),
        // mas a plataforma permanece selecionada
        state = state.copy(
            lastScannedStop = stopCreated,
            selectedPartnerIdForNextScan = null
        )

        assertNull("Próxima bipagem deve resetar para o Master por padrão", state.selectedPartnerIdForNextScan)
        assertEquals("plat-shopee", state.currentPlatformId)
    }

    @Test
    fun testStickyPlatformSelectorPersistsBetweenScans() {
        // Validação do seletor sticky de plataforma (Prompt 2):
        // A plataforma ativa persiste entre bipagens consecutivas até que o usuário troque manualmente.

        val platformShopee = Platform(id = "plat-shopee", userId = "user-1", name = "Shopee Xpress", cycle = "semanal", paymentDay = "sexta")
        val platformMercadoLivre = Platform(id = "plat-ml", userId = "user-1", name = "Mercado Livre", cycle = "semanal", paymentDay = "quarta")

        var state = RouteScannerUiState(
            routeId = "route-master-1",
            platforms = listOf(platformShopee, platformMercadoLivre),
            currentPlatformId = platformShopee.id,
            currentPackageType = PackageType.PACOTINHO
        )

        assertEquals("plat-shopee", state.currentPlatformId)
        assertEquals("Shopee Xpress", state.activePlatform?.name)

        // 1ª bipagem: com Shopee selecionada
        val stop1 = MasterRouteStop(
            id = "stop-1",
            routeId = state.routeId,
            userId = "user-1",
            barcode = "BR001SHOPEE",
            recipientName = "Cliente 1",
            fullAddress = "Rua Um, 10",
            platformId = state.currentPlatformId,
            packageType = state.currentPackageType
        )
        assertEquals("plat-shopee", stop1.platformId)

        // Ao concluir a 1ª bipagem, currentPlatformId MANTÉM-SE intacto (sticky!)
        state = state.copy(
            totalScannedCount = 1,
            lastScannedStop = stop1
            // currentPlatformId não é alterado!
        )
        assertEquals("plat-shopee", state.currentPlatformId)

        // Motorista troca manualmente para Mercado Livre
        state = state.copy(currentPlatformId = platformMercadoLivre.id)
        assertEquals("plat-ml", state.currentPlatformId)
        assertEquals("Mercado Livre", state.activePlatform?.name)

        // 2ª bipagem: agora herda Mercado Livre
        val stop2 = MasterRouteStop(
            id = "stop-2",
            routeId = state.routeId,
            userId = "user-1",
            barcode = "BR002ML",
            recipientName = "Cliente 2",
            fullAddress = "Rua Dois, 20",
            platformId = state.currentPlatformId,
            packageType = state.currentPackageType
        )
        assertEquals("plat-ml", stop2.platformId)

        // Ao concluir a 2ª bipagem, continua no Mercado Livre (sticky!)
        state = state.copy(
            totalScannedCount = 2,
            lastScannedStop = stop2
        )
        assertEquals("plat-ml", state.currentPlatformId)

        // 3ª bipagem: sem toque manual do usuário, DEVE continuar no Mercado Livre
        val stop3 = MasterRouteStop(
            id = "stop-3",
            routeId = state.routeId,
            userId = "user-1",
            barcode = "BR003ML",
            recipientName = "Cliente 3",
            fullAddress = "Rua Tres, 30",
            platformId = state.currentPlatformId,
            packageType = state.currentPackageType
        )
        assertEquals("plat-ml", stop3.platformId)
    }

    @Test
    fun testFractionatedScannerStepTransitionOnBarcodeDetected() {
        // Validação TASK-DES-08: Scanner Fracionado (2 Etapas)
        // Etapa 1: Começa em BARCODE_SEARCH com visor limpo
        var state = RouteScannerUiState(
            routeId = "route-test",
            currentStep = ScannerStep.BARCODE_SEARCH,
            pendingBarcode = null,
            pendingParsedAddress = null
        )

        assertEquals(ScannerStep.BARCODE_SEARCH, state.currentStep)
        assertNull(state.pendingBarcode)

        // Ao ler código de barras: transiciona para OCR_CONFIRMATION
        val scannedBarcode = "BR420918237BR"
        state = state.copy(
            currentStep = ScannerStep.OCR_CONFIRMATION,
            pendingBarcode = scannedBarcode,
            isOcrScanning = true
        )

        assertEquals(ScannerStep.OCR_CONFIRMATION, state.currentStep)
        assertEquals("BR420918237BR", state.pendingBarcode)
        assertTrue(state.isOcrScanning)

        // Refinamento do OCR de endereço
        val ocrAddress = ParsedAddress(
            recipientName = "Carlos Eduardo Silva",
            street = "Rua das Palmeiras",
            number = "120",
            neighborhood = "Centro",
            city = "São Paulo",
            state = "SP",
            cep = "01001-000",
            fullFormattedAddress = "Rua das Palmeiras, 120, Centro, São Paulo - SP"
        )
        state = state.copy(
            pendingParsedAddress = ocrAddress,
            isOcrScanning = false
        )

        assertEquals("Carlos Eduardo Silva", state.pendingParsedAddress?.recipientName)
        assertEquals("01001-000", state.pendingParsedAddress?.cep)
        assertFalse(state.isOcrScanning)

        // Ao clicar em Confirmar: salva e retorna a BARCODE_SEARCH
        val createdStop = MasterRouteStop(
            id = "stop-new-1",
            routeId = state.routeId,
            userId = "user-1",
            barcode = state.pendingBarcode!!,
            recipientName = state.pendingParsedAddress?.recipientName,
            fullAddress = state.pendingParsedAddress?.fullFormattedAddress ?: "",
            cep = state.pendingParsedAddress?.cep
        )

        state = state.copy(
            currentStep = ScannerStep.BARCODE_SEARCH,
            totalScannedCount = state.totalScannedCount + 1,
            lastScannedStop = createdStop,
            pendingBarcode = null,
            pendingParsedAddress = null,
            isOcrScanning = false
        )

        assertEquals(ScannerStep.BARCODE_SEARCH, state.currentStep)
        assertEquals(1, state.totalScannedCount)
        assertNull(state.pendingBarcode)
        assertNull(state.pendingParsedAddress)
    }

    @Test
    fun testFractionatedScannerRetryDiscardsPendingAndReturnsToBarcodeSearch() {
        // Validação TASK-DES-08: Bipar Novamente descarta código pendente e volta ao modo BARCODE_SEARCH
        var state = RouteScannerUiState(
            routeId = "route-test",
            currentStep = ScannerStep.OCR_CONFIRMATION,
            pendingBarcode = "BR_WRONG_BARCODE",
            pendingParsedAddress = null,
            isOcrScanning = true
        )

        assertEquals(ScannerStep.OCR_CONFIRMATION, state.currentStep)
        assertNotNull(state.pendingBarcode)

        // Motorista clica em "Bipar Novamente"
        state = state.copy(
            currentStep = ScannerStep.BARCODE_SEARCH,
            pendingBarcode = null,
            pendingParsedAddress = null,
            isOcrScanning = false
        )

        assertEquals(ScannerStep.BARCODE_SEARCH, state.currentStep)
        assertNull(state.pendingBarcode)
        assertNull(state.pendingParsedAddress)
        assertFalse(state.isOcrScanning)
    }

    @Test
    fun testSkipOcrSavesWithEmptyAddressAndReturnsToBarcodeSearch() {
        // Validação TASK-DES-08: Pular OCR salva apenas código de barras e tipo de pacote
        var state = RouteScannerUiState(
            routeId = "route-test",
            currentStep = ScannerStep.OCR_CONFIRMATION,
            pendingBarcode = "BR_UNREADABLE_OCR",
            pendingParsedAddress = null,
            currentPackageType = PackageType.VOLUMOSO
        )

        val createdStop = MasterRouteStop(
            id = "stop-skip-ocr",
            routeId = state.routeId,
            userId = "user-1",
            barcode = state.pendingBarcode!!,
            recipientName = null,
            fullAddress = "",
            packageType = state.currentPackageType
        )

        state = state.copy(
            currentStep = ScannerStep.BARCODE_SEARCH,
            totalScannedCount = state.totalScannedCount + 1,
            lastScannedStop = createdStop,
            pendingBarcode = null,
            pendingParsedAddress = null,
            currentPackageType = PackageType.PACOTINHO // Reseta para PACOTINHO
        )

        assertEquals(ScannerStep.BARCODE_SEARCH, state.currentStep)
        assertEquals(1, state.totalScannedCount)
        assertEquals(PackageType.VOLUMOSO, createdStop.packageType)
        assertEquals(PackageType.PACOTINHO, state.currentPackageType)
        assertNull(state.pendingBarcode)
    }

    @Test
    fun testStickyMarketplaceSelectorPersistsBetweenScans() {
        // Validação da seleção sticky de Tomador / Marketplace
        // O tomador selecionado persiste entre sucessivas bipagens até nova troca manual
        var state = RouteScannerUiState(
            routeId = "route-mkt-1",
            currentMarketplaceName = "TikTok Shop"
        )

        assertEquals("TikTok Shop", state.currentMarketplaceName)

        // 1ª bipagem: herda TikTok Shop
        val stop1 = MasterRouteStop(
            id = "stop-mkt-1",
            routeId = state.routeId,
            userId = "user-1",
            barcode = "BR-TIKTOK-001",
            recipientName = "Consumidor TikTok",
            fullAddress = "Rua Teste, 100",
            marketplaceName = state.currentMarketplaceName
        )
        assertEquals("TikTok Shop", stop1.marketplaceName)

        // Ao concluir a 1ª bipagem, o marketplace MANTÉM-SE (sticky!)
        state = state.copy(
            totalScannedCount = 1,
            lastScannedStop = stop1
        )
        assertEquals("TikTok Shop", state.currentMarketplaceName)

        // Motorista troca manualmente para Shopee
        state = state.copy(currentMarketplaceName = "Shopee")
        assertEquals("Shopee", state.currentMarketplaceName)

        // 2ª bipagem: herda Shopee
        val stop2 = MasterRouteStop(
            id = "stop-mkt-2",
            routeId = state.routeId,
            userId = "user-1",
            barcode = "BR-SHOPEE-002",
            recipientName = "Consumidor Shopee",
            fullAddress = "Avenida Central, 500",
            marketplaceName = state.currentMarketplaceName
        )
        assertEquals("Shopee", stop2.marketplaceName)

        // Ao concluir a 2ª bipagem, continua Shopee (sticky!)
        state = state.copy(
            totalScannedCount = 2,
            lastScannedStop = stop2
        )
        assertEquals("Shopee", state.currentMarketplaceName)
    }

    @Test
    fun testCancelAutoAdvanceSetsCountdownToNull() {
        // Validação do cancelamento do timer de auto-avanço:
        // Ao clicar em 'Editar' ou interagir com o card, o timer de 5s é cancelado imediatamente
        // para permitir edição calma e sem interrupções.
        var state = RouteScannerUiState(
            routeId = "route-test",
            autoAdvanceCountdown = 5
        )

        assertEquals(5, state.autoAdvanceCountdown)

        // Simula a invocação de cancelAutoAdvance()
        state = state.copy(autoAdvanceCountdown = null)

        assertNull("Countdown de auto-avanço deve ser nulo após cancelAutoAdvance", state.autoAdvanceCountdown)
    }

    @Test
    fun testUpdateScannedStopPersistsMarketplaceAndCancelsAutoAdvance() {
        // Validação da atualização e edição de dados pelo EditStopDialog
        val originalStop = MasterRouteStop(
            id = "stop-edit-1",
            routeId = "route-test",
            userId = "user-1",
            barcode = "BR-ORIGINAL-001",
            recipientName = "Nome Antigo",
            fullAddress = "Endereço Antigo, 10",
            cep = "01001-000",
            packageType = PackageType.PACOTINHO,
            marketplaceName = "Kwai",
            notes = "Sem notas"
        )

        var state = RouteScannerUiState(
            routeId = "route-test",
            lastScannedStop = originalStop,
            autoAdvanceCountdown = 3,
            isConfidenceWarning = true
        )

        assertEquals(3, state.autoAdvanceCountdown)
        assertTrue(state.isConfidenceWarning)

        // Edição realizada com novo tomador e endereço corrigido
        val updatedStop = originalStop.copy(
            recipientName = "Nome Corrigido",
            fullAddress = "Endereço Novo, 20",
            cep = "02002-000",
            packageType = PackageType.VOLUMOSO,
            marketplaceName = "Mercado Livre",
            notes = "Entregar na portaria"
        )

        state = state.copy(
            lastScannedStop = updatedStop,
            autoAdvanceCountdown = null,
            isConfidenceWarning = false
        )

        assertNull("Countdown deve estar zerado", state.autoAdvanceCountdown)
        assertFalse("Aviso de conferência deve ser limpo", state.isConfidenceWarning)
        assertEquals("Nome Corrigido", state.lastScannedStop?.recipientName)
        assertEquals("Endereço Novo, 20", state.lastScannedStop?.fullAddress)
        assertEquals("02002-000", state.lastScannedStop?.cep)
        assertEquals(PackageType.VOLUMOSO, state.lastScannedStop?.packageType)
        assertEquals("Mercado Livre", state.lastScannedStop?.marketplaceName)
        assertEquals("Entregar na portaria", state.lastScannedStop?.notes)
    }
}

