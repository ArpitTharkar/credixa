package com.arpit.myapplication.remote;

import androidx.annotation.Nullable;

import com.arpit.myapplication.ServiceLocator;
import com.arpit.myapplication.UserRepository;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = ServiceLocator.provideUserRepository().getSessionToken();
        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }
        Request.Builder b = original.newBuilder()
                .header("Authorization", "Bearer " + token);
        Request req = b.build();
        return chain.proceed(req);
    }
}
