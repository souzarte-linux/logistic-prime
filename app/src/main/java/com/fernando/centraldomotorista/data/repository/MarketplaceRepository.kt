package com.fernando.centraldomotorista.data.repository

import android.content.Context
import android.util.Log
import com.fernando.centraldomotorista.data.model.Marketplace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório responsável pelo fornecimento dos Marketplaces (e-commerces geradores de pacotes de entrega).
 * Fornece catálogo padrão offline pré-configurado e suporte a consultas, extensões e novas empresas cadastradas.
 */
open class MarketplaceRepository(private val context: Context? = null) {
    private val tag = "MarketplaceRepo"

    companion object {
        private const val PREFS_NAME = "custom_marketplaces_prefs"
        private const val KEY_CUSTOM_MARKETPLACES = "custom_marketplaces_set"

        private val inMemoryCustomMarketplaces = mutableListOf<Marketplace>()

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

        /**
         * Retorna os marketplaces customizados cadastrados pelo usuário.
         */
        fun getCustomMarketplaces(ctx: Context? = null): List<Marketplace> {
            val fromPrefs = if (ctx != null) {
                val sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val set = sp.getStringSet(KEY_CUSTOM_MARKETPLACES, emptySet()) ?: emptySet()
                set.map { name ->
                    val id = "custom_" + name.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
                    Marketplace(id = id, name = name, active = true)
                }
            } else emptyList()

            return (inMemoryCustomMarketplaces + fromPrefs).distinctBy { it.name.lowercase().trim() }
        }

        /**
         * Salva um novo marketplace customizado em memória e em SharedPreferences se o contexto for fornecido.
         */
        fun saveCustomMarketplace(name: String, ctx: Context? = null): Marketplace {
            val trimmed = name.trim()
            val id = "custom_" + trimmed.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
            val newMkt = Marketplace(id = id, name = trimmed, active = true)

            if (inMemoryCustomMarketplaces.none { it.name.equals(trimmed, ignoreCase = true) }) {
                inMemoryCustomMarketplaces.add(newMkt)
            }

            if (ctx != null) {
                val sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentSet = sp.getStringSet(KEY_CUSTOM_MARKETPLACES, emptySet())?.toMutableSet() ?: mutableSetOf()
                currentSet.add(trimmed)
                sp.edit().putStringSet(KEY_CUSTOM_MARKETPLACES, currentSet).apply()
            }

            return newMkt
        }
    }

    /**
     * Retorna a lista estática/padrão de marketplaces pré-configurados no aplicativo.
     */
    fun getDefaultMarketplaces(): List<Marketplace> {
        return DEFAULT_MARKETPLACES
    }

    /**
     * Retorna todos os marketplaces disponíveis (com fallback para lista padrão + customizados).
     */
    open suspend fun getMarketplaces(): List<Marketplace> = withContext(Dispatchers.IO) {
        try {
            val custom = getCustomMarketplaces(context)
            (DEFAULT_MARKETPLACES + custom).distinctBy { it.name.lowercase().trim() }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao obter marketplaces: ${e.message}", e)
            DEFAULT_MARKETPLACES
        }
    }

    /**
     * Cadastra e persiste uma nova empresa / tomador sob demanda.
     */
    open suspend fun addCustomMarketplace(name: String): Marketplace = withContext(Dispatchers.IO) {
        saveCustomMarketplace(name, context)
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
