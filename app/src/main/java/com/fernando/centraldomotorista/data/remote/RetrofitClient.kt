package com.fernando.centraldomotorista.data.remote

import com.fernando.centraldomotorista.data.remote.api.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NeonApiConfig.NEON_DATA_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val profileApi: ProfileApi by lazy { retrofit.create(ProfileApi::class.java) }
    val routeApi: RouteApi by lazy { retrofit.create(RouteApi::class.java) }
    val expenseApi: ExpenseApi by lazy { retrofit.create(ExpenseApi::class.java) }
    val dailyTotalApi: DailyTotalApi by lazy { retrofit.create(DailyTotalApi::class.java) }
    val partMaintenanceApi: PartMaintenanceApi by lazy { retrofit.create(PartMaintenanceApi::class.java) }
    val billingCycleApi: BillingCycleApi by lazy { retrofit.create(BillingCycleApi::class.java) }
    val notificationApi: NotificationApi by lazy { retrofit.create(NotificationApi::class.java) }
    val gasStationApi: GasStationApi by lazy { retrofit.create(GasStationApi::class.java) }
    val gasStationBrandApi: GasStationBrandApi by lazy { retrofit.create(GasStationBrandApi::class.java) }
    val creditCardApi: CreditCardApi by lazy { retrofit.create(CreditCardApi::class.java) }
    val cardBrandApi: CardBrandApi by lazy { retrofit.create(CardBrandApi::class.java) }
    val cardOperatorApi: CardOperatorApi by lazy { retrofit.create(CardOperatorApi::class.java) }
}
