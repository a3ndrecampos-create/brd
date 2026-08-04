package com.beautymanager.app.core.di

import com.beautymanager.app.BuildConfig
import com.beautymanager.app.data.remote.barcode.BarcodeApi
import com.beautymanager.app.data.remote.barcode.CosmosApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CosmosClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val OPEN_BEAUTY_FACTS_BASE_URL = "https://world.openbeautyfacts.org/"
    private const val COSMOS_BASE_URL = "https://api.cosmos.bluesoft.com.br/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

    @Provides
    @Singleton
    @CosmosClient
    fun provideCosmosOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Cosmos-Token", BuildConfig.COSMOS_API_TOKEN)
                    .header("Content-Type", "application/json")
                    .apply {
                        // A Cosmos exige um User-Agent específico fornecido no cadastro;
                        // sem ele, a API rejeita a chamada mesmo com o token certo.
                        if (BuildConfig.COSMOS_USER_AGENT.isNotBlank()) {
                            header("User-Agent", BuildConfig.COSMOS_USER_AGENT)
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideBarcodeApi(client: OkHttpClient, json: Json): BarcodeApi =
        Retrofit.Builder()
            .baseUrl(OPEN_BEAUTY_FACTS_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BarcodeApi::class.java)

    @Provides
    @Singleton
    fun provideCosmosApi(@CosmosClient client: OkHttpClient, json: Json): CosmosApi =
        Retrofit.Builder()
            .baseUrl(COSMOS_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CosmosApi::class.java)
}
