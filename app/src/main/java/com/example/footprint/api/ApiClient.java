package com.example.footprint.api;

import com.example.footprint.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static ApiService service;
    private static Retrofit retrofit;
    public static synchronized <T> T create(Class<T> type) { get(); return retrofit.create(type); }
    private static com.example.footprint.security.TokenManager tokens;
    public static synchronized void initialize(com.example.footprint.security.TokenManager store) {
        tokens = store;
        service = null;
    }
    private ApiClient() {}

    public static synchronized ApiService get() {
        if (service == null) {
            HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
            logger.setLevel(HttpLoggingInterceptor.Level.NONE);
            AuthApi refreshApi = new Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
                    .client(new OkHttpClient.Builder().retryOnConnectionFailure(false)
                            .followRedirects(false).followSslRedirects(false).build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(AuthApi.class);
            if (tokens == null) throw new IllegalStateException("ApiClient must be initialized by Application");
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        okhttp3.Request.Builder request = chain.request().newBuilder().header("Accept", "application/json");
                        com.example.footprint.data.model.AuthTokens pair = tokens.read();
                        String path = chain.request().url().encodedPath();
                        if (pair == null && !path.startsWith("/api/policies")) throw new java.io.IOException("Authentication required");
                        if (pair != null) request.header("Authorization", "Bearer " + pair.accessToken);
                        return chain.proceed(request.build());
                    })
                    .authenticator(new com.example.footprint.security.TokenAuthenticator(tokens, refreshApi))
                    .addInterceptor(logger)
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(ApiService.class);
        }
        return service;
    }
}
