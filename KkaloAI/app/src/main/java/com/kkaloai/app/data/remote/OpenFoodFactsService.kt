package com.kkaloai.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsService {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}

data class OpenFoodFactsResponse(
    val status: Int,
    val product: Product?
)

data class Product(
    val product_name: String?,
    val nutriments: Nutriments?
)

data class Nutriments(
    val energy_100g: Float?,
    val proteins_100g: Float?,
    val carbohydrates_100g: Float?,
    val fat_100g: Float?
)
