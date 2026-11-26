package com.fernando.centraldomotorista.costcenter

import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.AllocationStatus
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.CardAccountLink
import com.fernando.centraldomotorista.data.model.CardInvoicePayment
import com.fernando.centraldomotorista.data.model.CostCenterSettings
import com.fernando.centraldomotorista.data.model.ExpenseSettlement
import com.fernando.centraldomotorista.data.model.FixedCost
import com.fernando.centraldomotorista.data.model.FixedCostCategory
import com.fernando.centraldomotorista.data.model.FixedCostPayment
import com.fernando.centraldomotorista.data.model.FixedCostPeriodicity
import com.fernando.centraldomotorista.data.model.FixedCostVersion
import com.fernando.centraldomotorista.data.model.LedgerEntry
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.LedgerRefType
import com.fernando.centraldomotorista.data.model.ReserveAllocation
import com.fernando.centraldomotorista.data.model.ReserveAllocationDay
import com.fernando.centraldomotorista.data.model.ReserveMode
import com.fernando.centraldomotorista.data.model.SettlementKind
import com.fernando.centraldomotorista.data.model.SettlementStatus
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.data.remote.dto.BankAccountDto
import com.fernando.centraldomotorista.data.remote.dto.CardAccountLinkDto
import com.fernando.centraldomotorista.data.remote.dto.CardInvoicePaymentDto
import com.fernando.centraldomotorista.data.remote.dto.CostCenterSettingsDto
import com.fernando.centraldomotorista.data.remote.dto.ExpenseDto
import com.fernando.centraldomotorista.data.remote.dto.ExpenseSettlementDto
import com.fernando.centraldomotorista.data.remote.dto.FixedCostDto
import com.fernando.centraldomotorista.data.remote.dto.FixedCostPaymentDto
import com.fernando.centraldomotorista.data.remote.dto.FixedCostVersionDto
import com.fernando.centraldomotorista.data.remote.dto.ReserveAllocationDayDto
import com.fernando.centraldomotorista.data.remote.dto.ReserveAllocationDto
import com.fernando.centraldomotorista.data.remote.dto.VaultDto
import com.fernando.centraldomotorista.data.remote.dto.VaultLedgerDto
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.data.remote.dto.toOutboxJsonObject
import com.fernando.centraldomotorista.sync.OutboxPayloadSerializer
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class CostCenterModelsAndDtosTest {

    private val t1 = Instant.parse("2026-11-20T10:15:30Z")
    private val t2 = Instant.parse("2026-11-20T12:00:00Z")
    private val d1 = LocalDate.of(2026, 11, 20)
    private val d2 = LocalDate.of(2026, 12, 1)

    @Test
    fun roundTripDtoDominioEntidadePreservaBigDecimalComEscalaCorreta() {
        val gson = OutboxPayloadSerializer.gson

        // 1. BankAccount
        val account = BankAccount(
            id = "acc-1",
            userId = "usr-1",
            name = "Nubank",
            institution = "Nu Pagamentos",
            agency = "0001",
            accountLastDigits = "1234",
            accountType = AccountType.CORRENTE,
            informedBalance = BigDecimal("1234.50"),
            informedBalanceAt = t1,
            active = true,
            sortOrder = 1,
            createdAt = t1,
            updatedAt = t2,
            deletedAt = null,
            syncState = SyncState.SINCRONIZADO
        )
        val accountJson = OutboxPayloadSerializer.serialize(account.toDto().toOutboxJsonObject())
        val accountRt = gson.fromJson(accountJson, BankAccountDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(account, accountRt)
        assertEquals(2, accountRt.informedBalance!!.scale())

        // 2. Vault
        val vault = Vault(
            id = "vlt-1",
            userId = "usr-1",
            accountId = "acc-1",
            name = "Manutenção",
            purpose = VaultPurpose.MANUTENCAO,
            goalAmount = BigDecimal("2500.00"),
            initialBalance = BigDecimal("150.25"),
            trackingStartDate = d1,
            active = true,
            sortOrder = 0,
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val vaultJson = OutboxPayloadSerializer.serialize(vault.toDto().toOutboxJsonObject())
        val vaultRt = gson.fromJson(vaultJson, VaultDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(vault, vaultRt)
        assertEquals(2, vaultRt.goalAmount!!.scale())
        assertEquals(2, vaultRt.initialBalance.scale())

        // 3. LedgerEntry
        val ledger = LedgerEntry(
            id = "led-1",
            userId = "usr-1",
            vaultId = "vlt-1",
            operationId = "op-1",
            leg = 0,
            entryType = LedgerEntryType.APORTE,
            amount = BigDecimal("300.00"),
            occurredAt = t1,
            refType = LedgerRefType.MANUAL,
            refId = null,
            contraVaultId = null,
            reversesEntryId = null,
            note = "Aporte inicial",
            createdAt = t1,
            syncState = SyncState.SINCRONIZADO
        )
        val ledgerJson = OutboxPayloadSerializer.serialize(ledger.toDto().toOutboxJsonObject())
        val ledgerRt = gson.fromJson(ledgerJson, VaultLedgerDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(ledger, ledgerRt)
        assertEquals(2, ledgerRt.amount.scale())

        // 4. ExpenseSettlement
        val settlement = ExpenseSettlement(
            id = "set-1",
            userId = "usr-1",
            expenseId = "exp-1",
            kind = SettlementKind.PIX,
            accountId = "acc-1",
            vaultId = "vlt-1",
            amount = BigDecimal("100.00"),
            vaultAmount = BigDecimal("60.00"),
            accountAmount = BigDecimal("40.00"),
            status = SettlementStatus.CONFIRMADA,
            operationId = "op-2",
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val settlementJson = OutboxPayloadSerializer.serialize(settlement.toDto().toOutboxJsonObject())
        val settlementRt = gson.fromJson(settlementJson, ExpenseSettlementDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(settlement, settlementRt)
        assertEquals(2, settlementRt.amount.scale())
        assertEquals(2, settlementRt.vaultAmount.scale())
        assertEquals(2, settlementRt.accountAmount.scale())

        // 5. FixedCost
        val fixedCost = FixedCost(
            id = "fc-1",
            userId = "usr-1",
            name = "IPVA",
            category = FixedCostCategory.VEICULO_TRIBUTO,
            catalogKey = "ipva",
            periodicity = FixedCostPeriodicity.ANUAL,
            dueDay = 15,
            dueMonth = 1,
            startDate = d1,
            endDate = d2,
            vaultId = "vlt-1",
            isVehicleRelated = true,
            analytical = false,
            seeded = true,
            notes = "Parcela única",
            active = true,
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val fcJson = OutboxPayloadSerializer.serialize(fixedCost.toDto().toOutboxJsonObject())
        val fcRt = gson.fromJson(fcJson, FixedCostDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(fixedCost, fcRt)

        // 6. FixedCostVersion
        val version = FixedCostVersion(
            id = "fcv-1",
            userId = "usr-1",
            fixedCostId = "fc-1",
            amount = BigDecimal("980.40"),
            effectiveFrom = d1,
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val fcvJson = OutboxPayloadSerializer.serialize(version.toDto().toOutboxJsonObject())
        val fcvRt = gson.fromJson(fcvJson, FixedCostVersionDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(version, fcvRt)
        assertEquals(2, fcvRt.amount.scale())

        // 7. FixedCostPayment
        val fcPayment = FixedCostPayment(
            id = "fcp-1",
            userId = "usr-1",
            fixedCostId = "fc-1",
            competenceDate = d1,
            paidAt = t1,
            amount = BigDecimal("250.00"),
            sourceAccountId = "acc-1",
            sourceVaultId = "vlt-1",
            vaultAmount = BigDecimal("150.00"),
            accountAmount = BigDecimal("100.00"),
            operationId = "op-3",
            notes = "Pago em dia",
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val fcpJson = OutboxPayloadSerializer.serialize(fcPayment.toDto().toOutboxJsonObject())
        val fcpRt = gson.fromJson(fcpJson, FixedCostPaymentDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(fcPayment, fcpRt)
        assertEquals(2, fcpRt.amount.scale())

        // 8. CardInvoicePayment
        val invPayment = CardInvoicePayment(
            id = "cip-1",
            userId = "usr-1",
            cardId = "card-1",
            referenceMonth = LocalDate.of(2026, 11, 1),
            amount = BigDecimal("432.10"),
            paidAt = t1,
            sourceAccountId = "acc-1",
            sourceVaultId = "vlt-1",
            vaultAmount = BigDecimal("100.00"),
            accountAmount = BigDecimal("332.10"),
            operationId = "op-4",
            notes = "Fatura novembro",
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val cipJson = OutboxPayloadSerializer.serialize(invPayment.toDto().toOutboxJsonObject())
        val cipRt = gson.fromJson(cipJson, CardInvoicePaymentDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(invPayment, cipRt)
        assertEquals(2, cipRt.amount.scale())

        // 9. CostCenterSettings
        val settings = CostCenterSettings(
            userId = "usr-1",
            maintenanceVaultId = "vlt-1",
            wearProvisionEnabled = true,
            fixedCostProvisionEnabled = false,
            reserveMode = ReserveMode.WORKING_DAYS,
            plannedWeekdays = 62,
            daysOff = listOf("2026-11-25"),
            seededCatalogVersion = 2,
            dismissedSeedKeys = listOf("ipva"),
            reminderEnabled = true,
            reminderOffsets = listOf(5, 1, 0),
            costCenterStartedOn = d1,
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val settingsJson = OutboxPayloadSerializer.serialize(settings.toDto().toOutboxJsonObject())
        val settingsRt = gson.fromJson(settingsJson, CostCenterSettingsDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(settings, settingsRt)

        // 10. ReserveAllocation
        val allocation = ReserveAllocation(
            id = "ra-1",
            userId = "usr-1",
            billingCycleId = "bc-1",
            vaultId = "vlt-1",
            suggestedAmount = BigDecimal("45.67"),
            amount = BigDecimal("40.00"),
            netCapApplied = true,
            status = AllocationStatus.ATIVA,
            operationId = "op-5",
            undoneAt = null,
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val raJson = OutboxPayloadSerializer.serialize(allocation.toDto().toOutboxJsonObject())
        val raRt = gson.fromJson(raJson, ReserveAllocationDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(allocation, raRt)
        assertEquals(2, raRt.suggestedAmount.scale())
        assertEquals(2, raRt.amount.scale())

        // 11. ReserveAllocationDay (6 casas decimais!)
        val allocationDay = ReserveAllocationDay(
            id = "rad-1",
            userId = "usr-1",
            allocationId = "ra-1",
            day = d1,
            platformId = "uber",
            suggestedAmount = BigDecimal("12.345678"),
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val radJson = OutboxPayloadSerializer.serialize(allocationDay.toDto().toOutboxJsonObject())
        val radRt = gson.fromJson(radJson, ReserveAllocationDayDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(allocationDay, radRt)
        assertEquals(6, radRt.suggestedAmount.scale())

        // 12. CardAccountLink
        val link = CardAccountLink(
            id = "cal-1",
            userId = "usr-1",
            cardId = "card-1",
            accountId = "acc-1",
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        val linkJson = OutboxPayloadSerializer.serialize(link.toDto().toOutboxJsonObject())
        val linkRt = gson.fromJson(linkJson, CardAccountLinkDto::class.java)
            .toDomain(SyncState.SINCRONIZADO)
            .toEntity()
            .toDomain()
        assertEquals(link, linkRt)
    }

    @Test
    fun fromKeyDesconhecidoNaoQuebraERetornaFallbackSeguroEmTodosEnums() {
        val unknown = "chave_inexistente_futuro"

        assertEquals(AccountType.OUTRA, AccountType.fromKey(unknown))
        assertEquals(AccountType.OUTRA, AccountType.fromKey(null))
        assertNull(AccountType.fromKeyOrNull(null))
        assertNull(AccountType.fromKeyOrNull("   "))
        assertEquals(AccountType.OUTRA, AccountType.fromKeyOrNull(unknown))

        assertEquals(VaultPurpose.OUTRO, VaultPurpose.fromKey(unknown))
        assertEquals(VaultPurpose.OUTRO, VaultPurpose.fromKey(null))

        assertEquals(LedgerEntryType.AJUSTE, LedgerEntryType.fromKey(unknown))
        assertEquals(LedgerEntryType.AJUSTE, LedgerEntryType.fromKey(null))

        assertEquals(LedgerRefType.MANUAL, LedgerRefType.fromKey(unknown))
        assertEquals(LedgerRefType.MANUAL, LedgerRefType.fromKey(null))
        assertNull(LedgerRefType.fromKeyOrNull(null))
        assertEquals(LedgerRefType.MANUAL, LedgerRefType.fromKeyOrNull(unknown))

        assertEquals(SettlementKind.PIX, SettlementKind.fromKey(unknown))
        assertEquals(SettlementKind.PIX, SettlementKind.fromKey(null))

        assertEquals(SettlementStatus.PENDENTE_DESPESA, SettlementStatus.fromKey(unknown))
        assertEquals(SettlementStatus.PENDENTE_DESPESA, SettlementStatus.fromKey(null))

        assertEquals(FixedCostCategory.OUTRO, FixedCostCategory.fromKey(unknown))
        assertEquals(FixedCostCategory.OUTRO, FixedCostCategory.fromKey(null))

        assertEquals(FixedCostPeriodicity.MENSAL, FixedCostPeriodicity.fromKey(unknown))
        assertEquals(FixedCostPeriodicity.MENSAL, FixedCostPeriodicity.fromKey(null))

        assertEquals(ReserveMode.CALENDAR, ReserveMode.fromKey(unknown))
        assertEquals(ReserveMode.CALENDAR, ReserveMode.fromKey(null))

        assertEquals(AllocationStatus.ATIVA, AllocationStatus.fromKey(unknown))
        assertEquals(AllocationStatus.ATIVA, AllocationStatus.fromKey(null))

        assertEquals(SyncState.PENDENTE, SyncState.fromKey(unknown))
        assertEquals(SyncState.PENDENTE, SyncState.fromKey(null))
    }

    @Test
    fun expenseDtoAntigoContinuaDesserializandoNormalmente() {
        val legacyJson = """
            {
              "id": "exp-legacy-1",
              "user_id": "usr-1",
              "category": "combustivel",
              "title": "Gasolina Posto Shell",
              "vendor": "Posto Shell",
              "amount": 150.75,
              "liters": 25.50,
              "fuel_type": "gasolina_comum",
              "price_per_liter": 5.91,
              "odometer_km": 45200.0,
              "payment_method": "pix",
              "is_full_tank": true,
              "occurred_at": "2026-11-18T14:30:00-03:00",
              "created_at": "2026-11-18T14:30:05-03:00"
            }
        """.trimIndent()

        val dto = Gson().fromJson(legacyJson, ExpenseDto::class.java)
        assertEquals("exp-legacy-1", dto.id)
        assertEquals("usr-1", dto.userId)
        assertEquals("combustivel", dto.category)
        assertEquals("Gasolina Posto Shell", dto.title)
        assertEquals(0, BigDecimal("150.75").compareTo(dto.amount))
        assertEquals(0, BigDecimal("25.50").compareTo(dto.liters))
        assertTrue(dto.isFullTank)

        val domain = dto.toDomain()
        assertEquals("exp-legacy-1", domain.id)
        assertEquals("pix", domain.paymentMethod)
        assertEquals(0, BigDecimal("150.75").compareTo(domain.amount))
    }
}
