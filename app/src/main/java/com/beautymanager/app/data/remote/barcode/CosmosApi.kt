package com.beautymanager.app.data.remote.barcode

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface Retrofit para a Bluesoft Cosmos (https://cosmos.bluesoft.com.br) — base
 * de produtos brasileira com cobertura muito melhor de itens de mercado/farmácia/
 * cosméticos do que a Open Beauty Facts, mas exige cadastro para conseguir um token
 * (X-Cosmos-Token) e um User-Agent próprios. Ver README para como configurar.
 *
 * Endpoint: GET https://api.cosmos.bluesoft.com.br/gtins/{codigo}.json
 * Cabeçalhos obrigatórios: X-Cosmos-Token e User-Agent (fornecidos no cadastro).
 */
interface CosmosApi {
    @GET("gtins/{codigo}.json")
    suspend fun getProduct(@Path("codigo") code: String): CosmosGtinResponse
}

@Serializable
data class CosmosGtinResponse(
    val description: String? = null,
    val gtin: Long? = null,
    val thumbnail: String? = null,
    val brand: CosmosBrand? = null
)

@Serializable
data class CosmosBrand(
    val name: String? = null
)
