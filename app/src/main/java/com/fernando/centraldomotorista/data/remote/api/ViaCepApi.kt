package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.ViaCepDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepApi {
    @GET("{cep}/json/")
    suspend fun getAddressByCep(@Path("cep") cep: String): ViaCepDto

    companion object {
        private const val BASE_URL = "https://viacep.com.br/ws/"

        val instance: ViaCepApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ViaCepApi::class.java)
        }
    }
}
