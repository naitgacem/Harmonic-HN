package com.simon.harmonichackernews.di

import com.simon.harmonichackernews.network.AlgoliaService
import com.simon.harmonichackernews.network.FirebaseService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    @Provides
    @Named("algolia")
    fun provideRetrofitAlgolia(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://hn.algolia.com/api/v1/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Named("firebase")
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://hacker-news.firebaseio.com/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object algoliaService {
    @Provides
    fun provideAlgoliaService(
        @Named("algolia") retrofit: Retrofit
    ): AlgoliaService {
        return retrofit.create(AlgoliaService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object firebaseService {
    @Provides
    fun provideFirebaseService(
        @Named("firebase") retrofit: Retrofit
    ): FirebaseService {
        return retrofit.create(FirebaseService::class.java)
    }
}
