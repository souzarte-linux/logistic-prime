package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.DeliveryPartnerApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeliveryPartnerRepository(
    private val deliveryPartnerApi: DeliveryPartnerApi = RetrofitClient.deliveryPartnerApi
) {
    suspend fun getDeliveryPartners(userId: String): List<DeliveryPartner> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            deliveryPartnerApi.getDeliveryPartners(userFilter, "full_name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("DeliveryPartnerRepo", "Erro ao buscar parceiros de entrega: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveDeliveryPartner(partner: DeliveryPartner): DeliveryPartner = withContext(Dispatchers.IO) {
        val dto = partner.toDto()
        if (partner.id.isNotBlank()) {
            val updated = deliveryPartnerApi.updateDeliveryPartner("eq.${partner.id}", dto)
            updated.firstOrNull()?.toDomain() ?: partner
        } else {
            val created = deliveryPartnerApi.createDeliveryPartner(dto)
            created.firstOrNull()?.toDomain() ?: partner
        }
    }

    suspend fun updateActiveStatus(partner: DeliveryPartner, active: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val updatedPartner = partner.copy(active = active)
            val updated = deliveryPartnerApi.updateDeliveryPartner("eq.${partner.id}", updatedPartner.toDto())
            updated.isNotEmpty()
        } catch (e: Exception) {
            Log.e("DeliveryPartnerRepo", "Erro ao alternar status ativo do parceiro ${partner.id}: ${e.message}", e)
            false
        }
    }

    suspend fun deleteDeliveryPartner(partnerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            deliveryPartnerApi.deleteDeliveryPartner("eq.$partnerId")
            true
        } catch (e: Exception) {
            Log.e("DeliveryPartnerRepo", "Erro ao excluir parceiro $partnerId: ${e.message}", e)
            false
        }
    }
}
