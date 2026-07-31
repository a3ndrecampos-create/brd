package com.beautymanager.app.data.remote.barcode

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface Retrofit para a Open Beauty Facts (https://world.openbeautyfacts.org) —
 * projeto irmão da Open Food Facts, mas focado em cosméticos/higiene/perfumaria.
 * É uma base colaborativa e gratuita, então a cobertura varia por produto/região;
 * quando não encontrar, a tela de cadastro cai para o formulário manual, e uma vez
 * salvo localmente o código nunca precisa ser consultado de novo (fica no Room).
 */
interface BarcodeApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenBeautyFactsResponse
}

@Serializable
data class OpenBeautyFactsResponse(
    val status: Int = 0,
    val product: OpenBeautyFactsProduct? = null
)

@Serializable
data class OpenBeautyFactsProduct(
    val product_name: String? = null,
    val brands: String? = null,
    val image_url: String? = null,
    val image_front_url: String? = null
)
