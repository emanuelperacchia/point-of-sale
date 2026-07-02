package com.pos.android.core.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pos.android.BuildConfig
import com.pos.android.attendance.data.AttendanceApi
import com.pos.android.auth.data.AuthApi
import com.pos.android.auth.data.TokenRefreshApi
import com.pos.android.dashboard.data.DashboardApi
import com.pos.android.core.network.di.AuthOkHttpClient
import com.pos.android.core.network.di.AuthRetrofit
import com.pos.android.inventory.data.ProductApi
import com.pos.android.notification.data.NotificationApi
import com.pos.android.pos.data.PosApi
import com.pos.android.shifts.data.ShiftApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * OkHttpClient SIN TokenAuthenticator para evitar la dependencia circular:
     * TokenAuthenticator → AuthApi → Retrofit → OkHttpClient → TokenAuthenticator
     */
    @AuthOkHttpClient
    @Provides
    @Singleton
    fun provideAuthOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            // SIN authenticator — rompe el ciclo
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @AuthRetrofit
    @Provides
    @Singleton
    fun provideAuthRetrofit(@AuthOkHttpClient authOkHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * AuthApi para login, registro, getMe, etc.
     * Usa el Retrofit principal con authenticator (refresh automático en 401).
     */
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    /**
     * TokenRefreshApi exclusivo para TokenAuthenticator.
     * Usa el AuthRetrofit SIN authenticator para evitar el ciclo:
     * TokenAuthenticator → TokenRefreshApi → AuthRetrofit → OkHttpClient (sin authenticator)
     */
    @Provides
    @Singleton
    fun provideTokenRefreshApi(@AuthRetrofit retrofit: Retrofit): TokenRefreshApi {
        return retrofit.create(TokenRefreshApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProductApi(retrofit: Retrofit): ProductApi {
        return retrofit.create(ProductApi::class.java)
    }

    @Provides
    @Singleton
    fun providePosApi(retrofit: Retrofit): PosApi {
        return retrofit.create(PosApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAttendanceApi(retrofit: Retrofit): AttendanceApi {
        return retrofit.create(AttendanceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideShiftApi(retrofit: Retrofit): ShiftApi {
        return retrofit.create(ShiftApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi {
        return retrofit.create(DashboardApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi {
        return retrofit.create(NotificationApi::class.java)
    }
}
