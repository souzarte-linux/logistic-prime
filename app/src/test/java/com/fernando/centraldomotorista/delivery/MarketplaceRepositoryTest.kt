package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.repository.MarketplaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MarketplaceRepositoryTest {

    private lateinit var repository: MarketplaceRepository

    @Before
    fun setUp() {
        repository = MarketplaceRepository()
    }

    @Test
    fun testDefaultMarketplacesContainExpectedTenMarketplaces() = runBlocking {
        val list = repository.getMarketplaces()
        assertEquals(10, list.size)

        val names = list.map { it.name }
        assertTrue(names.contains("TikTok Shop"))
        assertTrue(names.contains("Kwai"))
        assertTrue(names.contains("Mercado Livre"))
        assertTrue(names.contains("Shopee"))
        assertTrue(names.contains("C&A"))
        assertTrue(names.contains("Riachuelo"))
        assertTrue(names.contains("Shein"))
        assertTrue(names.contains("Amazon"))
        assertTrue(names.contains("Magalu"))
        assertTrue(names.contains("Loja Virtual"))
    }

    @Test
    fun testGetActiveMarketplaces() = runBlocking {
        val activeList = repository.getActiveMarketplaces()
        assertFalse(activeList.isEmpty())
        assertTrue(activeList.all { it.active })
    }

    @Test
    fun testFindByNameCaseInsensitive() = runBlocking {
        val shopee = repository.findByName("shopee")
        assertNotNull(shopee)
        assertEquals("Shopee", shopee?.name)

        val ml = repository.findByName("MERCADO LIVRE")
        assertNotNull(ml)
        assertEquals("Mercado Livre", ml?.name)

        val tiktok = repository.findByName("  tiktok shop  ")
        assertNotNull(tiktok)
        assertEquals("TikTok Shop", tiktok?.name)

        val nonexistent = repository.findByName("Loja Inexistente 123")
        assertNull(nonexistent)

        val nullName = repository.findByName(null)
        assertNull(nullName)
    }

    @Test
    fun testGetDefaultMarketplacesStaticList() {
        val defaultList = repository.getDefaultMarketplaces()
        assertEquals(10, defaultList.size)
        assertEquals(MarketplaceRepository.DEFAULT_MARKETPLACES, defaultList)
    }
}
