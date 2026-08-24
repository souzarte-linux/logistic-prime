package com.fernando.centraldomotorista.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ViaCepDto(
    @SerializedName("cep")
    val cep: String? = null,
    @SerializedName("logradouro")
    val logradouro: String? = null,
    @SerializedName("complemento")
    val complemento: String? = null,
    @SerializedName("bairro")
    val bairro: String? = null,
    @SerializedName("localidade")
    val localidade: String? = null,
    @SerializedName("uf")
    val uf: String? = null,
    @SerializedName("erro")
    val erro: Boolean? = false
)
