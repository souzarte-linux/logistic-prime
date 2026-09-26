package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.remote.api.DeliveryPartnerSessionApi
import com.fernando.centraldomotorista.data.remote.dto.DeliveryPartnerSessionDto
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerSessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DeliveryPartnerSessionRepositoryTest {

    private class FakeDeliveryPartnerSessionApi : DeliveryPartnerSessionApi {
        val storage = mutableMapOf<String, DeliveryPartnerSessionDto>()
        var shouldThrowOnCycleQuery = false
        var shouldThrowOnUpdate = false

        override suspend fun getSessions(
            userIdFilter: String,
            cycleIdFilter: String?,
            order: String
        ): List<DeliveryPartnerSessionDto> {
            if (shouldThrowOnCycleQuery && cycleIdFilter != null) {
                throw RuntimeException("Simulated API failure on cycle query")
            }
            val userId = userIdFilter.removePrefix("eq.")
            var list = storage.values.filter { it.userId == userId }
            if (cycleIdFilter != null) {
                val cycleId = cycleIdFilter.removePrefix("eq.")
                list = list.filter { it.billingCycleId == cycleId }
            }
            return list
        }

        override suspend fun getSessionsForPartner(
            userIdFilter: String,
            partnerFilter: String,
            order: String
        ): List<DeliveryPartnerSessionDto> {
            val userId = userIdFilter.removePrefix("eq.")
            val partnerId = partnerFilter.removePrefix("eq.")
            return storage.values.filter { it.userId == userId && it.partnerId == partnerId }
        }

        override suspend fun getSessionById(idFilter: String): List<DeliveryPartnerSessionDto> {
            val id = idFilter.removePrefix("eq.")
            val item = storage[id]
            return if (item != null) listOf(item) else emptyList()
        }

        override suspend fun createSession(session: DeliveryPartnerSessionDto): List<DeliveryPartnerSessionDto> {
            val id = if (!session.id.isNullOrBlank()) session.id else "generated-${storage.size + 1}"
            val saved = session.copy(id = id)
            storage[id] = saved
            return listOf(saved)
        }

        override suspend fun updateSession(
            idFilter: String,
            session: DeliveryPartnerSessionDto
        ): List<DeliveryPartnerSessionDto> {
            if (shouldThrowOnUpdate) {
                throw RuntimeException("Simulated API failure on update")
            }
            val id = idFilter.removePrefix("eq.")
            storage[id] = session
            return listOf(session)
        }

        override suspend fun deleteSession(idFilter: String) {
            val id = idFilter.removePrefix("eq.")
            storage.remove(id)
        }

        override suspend fun getSessionByExpenseId(expenseIdFilter: String): List<DeliveryPartnerSessionDto> {
            val expenseId = expenseIdFilter.removePrefix("eq.")
            return storage.values.filter { it.expenseId == expenseId }
        }
    }

    private fun createSampleSession(
        id: String,
        userId: String = "user-1",
        billingCycleId: String? = null
    ): DeliveryPartnerSession {
        return DeliveryPartnerSession(
            id = id,
            userId = userId,
            partnerId = "partner-10",
            platformId = "plat-shopee",
            billingCycleId = billingCycleId,
            expectedPackageCount = 50,
            deliveredCount = 48,
            returnedCount = 2,
            amountPaid = BigDecimal("240.00"),
            startTime = OffsetDateTime.of(2026, 9, 20, 8, 0, 0, 0, ZoneOffset.UTC)
        )
    }

    @Test
    fun testGetSessionsByBillingCycleDirectApiSuccess() = runBlocking {
        val fakeApi = FakeDeliveryPartnerSessionApi()
        val s1 = createSampleSession("s-1", "user-1", "cycle-100").toDto()
        val s2 = createSampleSession("s-2", "user-1", "cycle-100").toDto()
        val s3 = createSampleSession("s-3", "user-1", "cycle-200").toDto()
        val s4 = createSampleSession("s-4", "user-2", "cycle-100").toDto()

        fakeApi.storage["s-1"] = s1
        fakeApi.storage["s-2"] = s2
        fakeApi.storage["s-3"] = s3
        fakeApi.storage["s-4"] = s4

        val repository = DeliveryPartnerSessionRepository(sessionApi = fakeApi)

        val resultCycle100 = repository.getSessionsByBillingCycle("user-1", "cycle-100")
        assertEquals(2, resultCycle100.size)
        assertTrue(resultCycle100.all { it.billingCycleId == "cycle-100" && it.userId == "user-1" })
        assertEquals(listOf("s-1", "s-2"), resultCycle100.map { it.id })

        val resultCycle200 = repository.getSessionsByBillingCycle("user-1", "cycle-200")
        assertEquals(1, resultCycle200.size)
        assertEquals("s-3", resultCycle200.first().id)

        val resultCycleNotFound = repository.getSessionsByBillingCycle("user-1", "cycle-999")
        assertTrue(resultCycleNotFound.isEmpty())
    }

    @Test
    fun testGetSessionsByBillingCycleFallbackOnApiError() = runBlocking {
        val fakeApi = FakeDeliveryPartnerSessionApi()
        val s1 = createSampleSession("s-1", "user-1", "cycle-target").toDto()
        val s2 = createSampleSession("s-2", "user-1", null).toDto()
        fakeApi.storage["s-1"] = s1
        fakeApi.storage["s-2"] = s2

        // Ativa falha na consulta direta por ciclo
        fakeApi.shouldThrowOnCycleQuery = true

        val repository = DeliveryPartnerSessionRepository(sessionApi = fakeApi)

        // Deve executar fallback via getSessions(userId).filter { it.billingCycleId == cycleId }
        val result = repository.getSessionsByBillingCycle("user-1", "cycle-target")
        assertEquals(1, result.size)
        assertEquals("s-1", result.first().id)
        assertEquals("cycle-target", result.first().billingCycleId)
    }

    @Test
    fun testUpdateSessionBillingCycleSuccess() = runBlocking {
        val fakeApi = FakeDeliveryPartnerSessionApi()
        val session = createSampleSession("s-10", "user-1", null)
        fakeApi.storage["s-10"] = session.toDto()

        val repository = DeliveryPartnerSessionRepository(sessionApi = fakeApi)

        // 1. Vincular a um ciclo
        val linkSuccess = repository.updateSessionBillingCycle("s-10", "cycle-alpha")
        assertTrue(linkSuccess)
        assertEquals("cycle-alpha", fakeApi.storage["s-10"]?.billingCycleId)

        // 2. Desvincular do ciclo (definir como null)
        val unlinkSuccess = repository.updateSessionBillingCycle("s-10", null)
        assertTrue(unlinkSuccess)
        assertNull(fakeApi.storage["s-10"]?.billingCycleId)
    }

    @Test
    fun testUpdateSessionBillingCycleReturnsFalseForNonExistentSession() = runBlocking {
        val fakeApi = FakeDeliveryPartnerSessionApi()
        val repository = DeliveryPartnerSessionRepository(sessionApi = fakeApi)

        val result = repository.updateSessionBillingCycle("session-inexistente", "cycle-1")
        assertFalse(result)
        assertTrue(fakeApi.storage.isEmpty())
    }

    @Test
    fun testUpdateSessionBillingCycleReturnsFalseOnApiError() = runBlocking {
        val fakeApi = FakeDeliveryPartnerSessionApi()
        val session = createSampleSession("s-err", "user-1", "cycle-old")
        fakeApi.storage["s-err"] = session.toDto()
        fakeApi.shouldThrowOnUpdate = true

        val repository = DeliveryPartnerSessionRepository(sessionApi = fakeApi)

        val result = repository.updateSessionBillingCycle("s-err", "cycle-new")
        assertFalse(result)
        // O ciclo não deve ter sido alterado com sucesso
        assertEquals("cycle-old", fakeApi.storage["s-err"]?.billingCycleId)
    }
}
