package com.fernando.centraldomotorista.ui.screens.historico

import com.fernando.centraldomotorista.data.model.TransactionItem
import com.fernando.centraldomotorista.data.model.TransactionSourceType
import com.fernando.centraldomotorista.data.model.TransactionType
import java.util.Locale

data class CategoryFilterOption(
    val key: String,
    val label: String,
    val group: String,
    val count: Int = 0
)

enum class MaintenanceSubtype(val label: String) {
    PECA("Peça"),
    OLEO("Óleo"),
    SERVICO("Serviço"),
    OUTROS("Outros")
}

/**
 * Heurística determinística (best-effort) para classificar despesas de manutenção
 * em Peça, Óleo, Serviço ou Outros a partir dos campos existentes (título, descrição,
 * marca, modelo, meta).
 */
fun inferMaintenanceSubtype(item: TransactionItem): MaintenanceSubtype {
    val exp = item.rawExpense
    val combinedText = listOfNotNull(
        item.title,
        item.subtitle,
        exp?.title,
        exp?.description,
        item.meta1,
        exp?.partBrand,
        exp?.partModel
    ).joinToString(" ").lowercase(Locale.ROOT)

    // 1. Óleo
    val oilKeywords = listOf(
        "óleo", "oleo", "lubrificante", "motul", "mobil", "havoline", "lubrax",
        "castrol", "yamalube", "elf", "20w50", "10w40", "10w30", "5w30", "5w40"
    )
    if (oilKeywords.any { combinedText.contains(it) }) {
        return MaintenanceSubtype.OLEO
    }

    // 2. Termos de Serviço / Mão de Obra
    val serviceKeywords = listOf(
        "mão de obra", "mao de obra", "mão-de-obra", "mao-de-obra",
        "serviço", "servico", "revisão", "revisao",
        "alinhamento", "balanceamento", "instalação", "instalacao",
        "conserto", "reparo", "lavagem", "guincho", "borracharia",
        "geometria", "regulagem", "socorro", "mecanic"
    )

    // 3. Termos de Peças e Componentes
    val partKeywords = listOf(
        "peça", "peca", "pastilha", "pneu", "vela", "filtro", "disco", "correia",
        "bateria", "amortecedor", "lâmpada", "lampada", "cabo", "relação", "relacao",
        "corrente", "coroa", "pinhão", "pinhao", "freio", "bucha", "coxim",
        "embreagem", "suspensão", "suspensao", "rolamento", "sensor", "escapamento",
        "farol", "retrovisor", "câmara", "camara", "raio", "aro", "guidão", "guidao",
        "carburador", "injeção", "injecao", "bobina", "estator", "retificador",
        "retentor", "junta", "bomba", "radiador", "terminal"
    )

    val hasBrandOrModel = !exp?.partBrand.isNullOrBlank() || !exp?.partModel.isNullOrBlank()
    val isExplicitService = serviceKeywords.any { combinedText.contains(it) }
    val isExplicitPart = partKeywords.any { combinedText.contains(it) } || hasBrandOrModel

    return when {
        isExplicitPart && !isExplicitService -> MaintenanceSubtype.PECA
        isExplicitService && !isExplicitPart -> MaintenanceSubtype.SERVICO
        isExplicitPart && isExplicitService -> {
            // Se contiver termos fortes de serviço manual (mão de obra, revisão), prioriza serviço
            if (combinedText.contains("mão de obra") || combinedText.contains("mao de obra") ||
                combinedText.contains("serviço") || combinedText.contains("servico")
            ) {
                MaintenanceSubtype.SERVICO
            } else {
                MaintenanceSubtype.PECA
            }
        }
        else -> MaintenanceSubtype.OUTROS
    }
}

fun extractMealType(item: TransactionItem): String {
    val rawMeal = item.rawExpense?.mealType?.trim()?.ifBlank { null }
    if (rawMeal != null) return rawMeal

    val text = "${item.title} ${item.subtitle} ${item.rawExpense?.description ?: ""}".lowercase(Locale.ROOT)
    return when {
        text.contains("almoço") || text.contains("almoco") -> "Almoço"
        text.contains("jantar") || text.contains("janta") -> "Jantar"
        text.contains("café") || text.contains("cafe") -> "Café Manhã"
        text.contains("lanche") -> "Lanche"
        else -> "Outros"
    }
}

fun extractFuelType(item: TransactionItem): String {
    val rawFuel = item.rawExpense?.fuelType?.trim()?.ifBlank { null }
    if (rawFuel != null) return rawFuel

    val text = "${item.title} ${item.subtitle} ${item.meta1 ?: ""} ${item.rawExpense?.description ?: ""}".lowercase(Locale.ROOT)
    return when {
        text.contains("gasolina aditivada") -> "Gasolina Aditivada"
        text.contains("gasolina comum") || text.contains("gasolina") -> "Gasolina Comum"
        text.contains("etanol") || text.contains("álcool") || text.contains("alcool") -> "Etanol"
        text.contains("diesel") -> "Diesel"
        text.contains("gnv") -> "GNV"
        else -> "Outros"
    }
}

fun extractPartnerName(item: TransactionItem): String {
    return item.rawPartner?.fullName?.trim()?.ifBlank { null }
        ?: item.establishment?.trim()?.ifBlank { null }
        ?: item.rawExpense?.vendor?.trim()?.ifBlank { null }
        ?: "Parceiro"
}

/**
 * Mapeia qualquer [TransactionItem] para sua respectiva opção granular de filtro de categoria.
 */
fun resolveTransactionCategoryFilter(item: TransactionItem): CategoryFilterOption {
    if (item.type == TransactionType.GANHO ||
        item.sourceType == TransactionSourceType.ROUTE ||
        item.sourceType == TransactionSourceType.DAILY_TOTAL
    ) {
        val plat = item.establishment?.trim()?.ifBlank { null }
            ?: item.title.trim().ifBlank { null }
            ?: "Avulso"
        return CategoryFilterOption(
            key = "GANHO_${plat.uppercase(Locale.ROOT)}",
            label = "Ganhos - $plat",
            group = "Ganhos"
        )
    }

    val catLower = (item.rawExpense?.category ?: item.category).lowercase(Locale.ROOT)
    val isEquipe = catLower in listOf("equipe", "parceiro", "entregador") ||
            item.rawPartnerSession != null ||
            item.rawPartner != null

    return when {
        isEquipe -> {
            val partner = extractPartnerName(item)
            CategoryFilterOption(
                key = "EQUIPE_${partner.uppercase(Locale.ROOT)}",
                label = "Equipe - $partner",
                group = "Equipe"
            )
        }
        catLower.contains("manuten") || catLower.contains("peca") || catLower.contains("peça") -> {
            val subtype = inferMaintenanceSubtype(item)
            CategoryFilterOption(
                key = "MANUTENCAO_${subtype.name}",
                label = "Manutenção - ${subtype.label}",
                group = "Manutenção"
            )
        }
        catLower.contains("alimenta") -> {
            val meal = extractMealType(item)
            CategoryFilterOption(
                key = "ALIMENTACAO_${meal.uppercase(Locale.ROOT)}",
                label = "Alimentação - $meal",
                group = "Alimentação"
            )
        }
        catLower.contains("combust") || catLower.contains("abastec") -> {
            val fuel = extractFuelType(item)
            CategoryFilterOption(
                key = "COMBUSTIVEL_${fuel.uppercase(Locale.ROOT)}",
                label = "Abastecimento - $fuel",
                group = "Abastecimento"
            )
        }
        else -> {
            val rawCat = item.category.ifBlank { "Outros" }
            val formatted = rawCat.lowercase(Locale.ROOT)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            CategoryFilterOption(
                key = "OUTROS_${rawCat.uppercase(Locale.ROOT)}",
                label = formatted,
                group = "Outros"
            )
        }
    }
}

/**
 * Deriva dinamicamente as opções de filtro de categoria disponíveis a partir das transações reais,
 * garantindo contagem e ausência de opções vazias.
 */
fun deriveAvailableCategoryFilters(items: List<TransactionItem>): List<CategoryFilterOption> {
    val countMap = mutableMapOf<String, Int>()
    val optionMap = mutableMapOf<String, CategoryFilterOption>()

    items.forEach { item ->
        val opt = resolveTransactionCategoryFilter(item)
        countMap[opt.key] = (countMap[opt.key] ?: 0) + 1
        optionMap[opt.key] = opt
    }

    fun groupOrder(group: String): Int = when (group) {
        "Manutenção" -> 1
        "Abastecimento" -> 2
        "Alimentação" -> 3
        "Equipe" -> 4
        "Ganhos" -> 5
        else -> 6
    }

    return optionMap.values
        .map { it.copy(count = countMap[it.key] ?: 0) }
        .sortedWith(compareBy({ groupOrder(it.group) }, { it.label }))
}
