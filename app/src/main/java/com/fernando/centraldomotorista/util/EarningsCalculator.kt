package com.fernando.centraldomotorista.util

import com.fernando.centraldomotorista.data.model.DailyTotal
import com.fernando.centraldomotorista.data.model.Route
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * Calculadora utilitária unificada de ganhos e rotas.
 * Resolve o cálculo do campo `subtract_routes` para garantir que
 * totais diários que descontam rotas do dia sejam abatidos corretamente,
 * nunca gerando valores negativos e utilizando o fuso local do dispositivo.
 */
object EarningsCalculator {

    /**
     * Calcula o valor líquido de um [DailyTotal] específico, considerando as rotas
     * ocorridas na mesma data local do dispositivo.
     *
     * Se [dailyTotal.subtractRoutes] for verdadeiro:
     *   líquido = max(0, dailyTotal.amount - soma(amount + tip + bonus de todas as rotas do mesmo dia local))
     * Caso contrário:
     *   líquido = dailyTotal.amount
     */
    fun calcularGanhoLiquidoDoDia(
        dailyTotal: DailyTotal,
        routes: List<Route>,
        zone: ZoneId = ZoneId.systemDefault()
    ): BigDecimal {
        if (!dailyTotal.subtractRoutes) {
            return dailyTotal.amount
        }

        val dailyLocalDate = dailyTotal.occurredAt.atZoneSameInstant(zone).toLocalDate()

        val routesDoMesmoDia = routes.filter {
            it.occurredAt.atZoneSameInstant(zone).toLocalDate() == dailyLocalDate
        }

        val totalRoutesAmount = routesDoMesmoDia
            .map { it.amount.add(it.tip).add(it.bonus) }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        val liquido = dailyTotal.amount.subtract(totalRoutesAmount)
        return maxOf(BigDecimal.ZERO, liquido)
    }

    /**
     * Calcula a soma total de ganhos para uma lista de rotas e totais diários
     * (geralmente filtrados para o mesmo dia ou período).
     *
     * somaTotal = soma(rotas.amount + tip + bonus) + soma(calcularGanhoLiquidoDoDia(dt, rotas))
     */
    fun calcularTotalGanhos(
        routes: List<Route>,
        dailyTotals: List<DailyTotal>,
        zone: ZoneId = ZoneId.systemDefault()
    ): BigDecimal {
        val totalRoutes = routes
            .map { it.amount.add(it.tip).add(it.bonus) }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        val totalDailyNet = dailyTotals
            .map { dt -> calcularGanhoLiquidoDoDia(dt, routes, zone) }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        return totalRoutes.add(totalDailyNet)
    }
}
