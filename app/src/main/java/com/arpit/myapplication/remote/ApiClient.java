package com.arpit.myapplication.remote;

import android.os.Build;

import com.arpit.myapplication.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public final class ApiClient {
    private static final String EMULATOR_BASE_URL = "http://10.0.2.2:8081/api/";

    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static BackendApi backendApi() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new TokenInterceptor())
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

            retrofit = new Retrofit.Builder()
                .baseUrl(resolveBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit.create(BackendApi.class);
    }

    private static String resolveBaseUrl() {
        // Emulator should use 10.0.2.2, otherwise use the configured server base URL
        boolean isEmulator = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.toLowerCase().contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86");
        if (isEmulator) {
            return EMULATOR_BASE_URL;
        }
        // BuildConfig.SERVER_BASE_URL is set in app/build.gradle.kts for debug/release
        return BuildConfig.SERVER_BASE_URL;
    }
}
