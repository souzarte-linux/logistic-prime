package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Marketplace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório responsável pelo fornecimento dos Marketplaces (e-commerces geradores de pacotes de entrega).
 * Fornece catálogo padrão offline pré-configurado e suporte a consultas e extensões futuras.
 */
open class MarketplaceRepository {
    private val tag = "MarketplaceRepo"

    companion object {
        val DEFAULT_MARKETPLACES: List<Marketplace> = listOf(
            Marketplace(id = "tiktok", name = "TikTok Shop", active = true),
            Marketplace(id = "kwai", name = "Kwai", active = true),
            Marketplace(id = "mercadolivre", name = "Mercado Livre", active = true),
            Marketplace(id = "shopee", name = "Shopee", active = true),
            Marketplace(id = "cea", name = "C&A", active = true),
            Marketplace(id = "riachuelo", name = "Riachuelo", active = true),
            Marketplace(id = "shein", name = "Shein", active = true),
            Marketplace(id = "amazon", name = "Amazon", active = true),
            Marketplace(id = "magalu", name = "Magalu", active = true),
            Marketplace(id = "loja_virtual", name = "Loja Virtual", active = true)
        )
    }

    /**
     * Retorna a lista estática/padrão de marketplaces pré-configurados no aplicativo.
     */
    fun getDefaultMarketplaces(): List<Marketplace> {
        return DEFAULT_MARKETPLACES
    }

    /**
     * Retorna todos os marketplaces disponíveis (com fallback para lista padrão).
     */
    open suspend fun getMarketplaces(): List<Marketplace> = withContext(Dispatchers.IO) {
        try {
            DEFAULT_MARKETPLACES
        } catch (e: Exception) {
            Log.e(tag, "Erro ao obter marketplaces: ${e.message}", e)
            DEFAULT_MARKETPLACES
        }
    }

    /**
     * Retorna apenas os marketplaces ativos.
     */
    open suspend fun getActiveMarketplaces(): List<Marketplace> = withContext(Dispatchers.IO) {
        getMarketplaces().filter { it.active }
    }

    /**
     * Busca um marketplace por seu nome exato ou aproximado (case-insensitive).
     */
    open suspend fun findByName(name: String?): Marketplace? = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext null
        val trimmed = name.trim()
        getMarketplaces().firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    }
}
