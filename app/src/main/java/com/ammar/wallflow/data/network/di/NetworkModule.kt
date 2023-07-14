package com.ammar.wallflow.data.network.di

import com.ammar.wallflow.BuildConfig
import com.ammar.wallflow.WALLHAVEN_BASE_URL
import com.ammar.wallflow.data.network.WallHavenNetworkDataSource
import com.ammar.wallflow.data.network.retrofit.DocumentConverterFactory
import com.ammar.wallflow.data.network.retrofit.RetrofitWallHavenNetwork
import com.ammar.wallflow.data.network.retrofit.WallHavenInterceptor
import com.ammar.wallflow.data.network.retrofit.api.WallHavenNetworkApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        wallHavenInterceptor: WallHavenInterceptor,
    ) = OkHttpClient.Builder().apply {
        readTimeout(30, TimeUnit.SECONDS)
        addInterceptor(wallHavenInterceptor)
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BASIC)
                }
            )
        }
    }.build()

    @Provides
    @Singleton
    fun providesWallhavenNetworkApi(
        networkJson: Json,
        okHttpClient: OkHttpClient,
    ): WallHavenNetworkApi =
        Retrofit.Builder().apply {
            baseUrl(WALLHAVEN_BASE_URL)
            client(okHttpClient)
            addConverterFactory(DocumentConverterFactory())
            addConverterFactory(
                networkJson.asConverterFactory("application/json".toMediaType())
            )
        }
            .build()
            .create(WallHavenNetworkApi::class.java)

    @Provides
    @Singleton
    fun providesWallHavenNetworkDataSource(wallHavenNetworkApi: WallHavenNetworkApi): WallHavenNetworkDataSource =
        RetrofitWallHavenNetwork(wallHavenNetworkApi)
}