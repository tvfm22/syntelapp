package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object NetworkModule {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createApiService(tokenProvider: () -> String?): SyntelApiService {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Accept", "application/json")
            val token = tokenProvider()
            if (!token.isNullOrBlank() && !original.url.encodedPath.endsWith("/auth/login/")) {
                builder.header("Authorization", "Token $token")
            }
            chain.proceed(builder.build())
        }

        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()

        val baseUrl = BuildConfig.SYNTEL_BASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SyntelApiService::class.java)
    }

    fun parseErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> {
                val rawBody = try {
                    error.response()?.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                val parsed = extractDrfMessage(rawBody)
                if (!parsed.isNullOrBlank()) {
                    parsed
                } else {
                    when (error.code()) {
                        401 -> "نشست شما منقضی شده یا اطلاعات ورود نادرست است."
                        403 -> "شما دسترسی لازم برای این عملیات را ندارید."
                        404 -> "آدرس سرویس در سرور سایت یافت نشد (مطمئن شوید آخرین نسخه API موبایل روی سرور جنگو مستقر شده است)."
                        in 500..599 -> "خطای داخلی سرور Syntel. لطفاً لحظاتی دیگر تلاش کنید."
                        else -> "خطا در ارتباط با سرور (کد ${error.code()})."
                    }
                }
            }
            is UnknownHostException -> "اتصال به سرور برقرار نشد؛ اینترنت دستگاه را بررسی کنید."
            is SocketTimeoutException -> "پاسخ‌گویی سرور زمان‌بر شد. لطفاً مجدداً تلاش کنید."
            is IOException -> "خطای شبکه رخ داد. در حالت آفلاین می‌توانید لیست‌های ذخیره‌شده را مشاهده کنید."
            else -> error.localizedMessage ?: "خطای غیرمنتظره رخ داد."
        }
    }

    private fun extractDrfMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(rawBody)
            when {
                json.has("detail") -> json.optString("detail")
                json.has("non_field_errors") -> json.optJSONArray("non_field_errors")?.optString(0)
                else -> {
                    val keys = json.keys()
                    if (keys.hasNext()) {
                        val firstKey = keys.next()
                        val arr = json.optJSONArray(firstKey)
                        arr?.optString(0) ?: json.optString(firstKey)
                    } else {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
