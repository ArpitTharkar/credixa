package com.arpit.myapplication;

import android.content.Context;

import com.arpit.myapplication.remote.ApiClient;
import com.arpit.myapplication.remote.BackendApi;

public final class ServiceLocator {
    private static Context appContext;
    private static BackendApi backendApi;
    private static WalletRepository walletRepository;
    private static UserRepository userRepository;

    private ServiceLocator() {}

    public static synchronized void init(Context context) {
        if (appContext == null) appContext = context.getApplicationContext();
    }

    public static synchronized BackendApi provideBackendApi() {
        if (backendApi == null) backendApi = ApiClient.backendApi();
        return backendApi;
    }

    public static synchronized WalletRepository provideWalletRepository() {
        if (walletRepository == null) walletRepository = new WalletRepository(appContext);
        return walletRepository;
    }

    public static synchronized UserRepository provideUserRepository() {
        if (userRepository == null) userRepository = new UserRepository(appContext);
        return userRepository;
    }
}
