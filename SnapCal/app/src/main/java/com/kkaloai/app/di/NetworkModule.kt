package com.kkaloai.app.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.kkaloai.app.data.remote.OpenFoodFactsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideGroqApiService(): com.kkaloai.app.data.remote.GroqApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(com.kkaloai.app.data.remote.GroqApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideOpenFoodFactsService(retrofit: Retrofit): OpenFoodFactsService =
        retrofit.create(OpenFoodFactsService::class.java)
}
