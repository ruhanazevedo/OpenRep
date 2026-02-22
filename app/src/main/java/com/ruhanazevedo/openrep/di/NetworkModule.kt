package com.ruhanazevedo.openrep.di

import com.ruhanazevedo.openrep.data.remote.RemoteMediaConfigService
import com.ruhanazevedo.openrep.data.remote.WgerApiService
import com.ruhanazevedo.openrep.data.remote.YouTubeApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YouTubeRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WgerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RawGitHubRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    @YouTubeRetrofit
    fun provideYouTubeRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideYouTubeApiService(@YouTubeRetrofit retrofit: Retrofit): YouTubeApiService =
        retrofit.create(YouTubeApiService::class.java)

    @Provides
    @Singleton
    @WgerRetrofit
    fun provideWgerRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://wger.de/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideWgerApiService(@WgerRetrofit retrofit: Retrofit): WgerApiService =
        retrofit.create(WgerApiService::class.java)

    @Provides
    @Singleton
    @RawGitHubRetrofit
    fun provideRawGitHubRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideRemoteMediaConfigService(@RawGitHubRetrofit retrofit: Retrofit): RemoteMediaConfigService =
        retrofit.create(RemoteMediaConfigService::class.java)
}
