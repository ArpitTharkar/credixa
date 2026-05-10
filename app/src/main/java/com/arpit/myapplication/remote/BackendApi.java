package com.arpit.myapplication.remote;

import com.arpit.myapplication.remote.model.AuthBridgeRequest;
import com.arpit.myapplication.remote.model.AuthBridgeResponse;
import com.arpit.myapplication.remote.model.LoginRequest;
import com.arpit.myapplication.remote.model.RequestOtpRequest;
import com.arpit.myapplication.remote.model.RequestOtpResponse;
import com.arpit.myapplication.remote.model.RegisterRequest;
import com.arpit.myapplication.remote.model.ResolveUserResponse;
import com.arpit.myapplication.remote.model.WalletResponse;
import com.arpit.myapplication.remote.model.AddMoneyRequest;
import com.arpit.myapplication.remote.model.TransferRequest;
import com.arpit.myapplication.remote.model.TransferResponse;
import com.arpit.myapplication.remote.model.TransactionDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

public interface BackendApi {
    /** Phase 2: used after Firebase OTP verifies phone (no password). */
    @POST("auth/phone-login")
    Call<AuthBridgeResponse> phoneLogin(@Body AuthBridgeRequest request);

    @POST("auth/register")
    Call<AuthBridgeResponse> register(@Body RegisterRequest request);

    @POST("auth/register/request-otp")
    Call<RequestOtpResponse> requestRegistrationOtp(@Body RequestOtpRequest request);

    @POST("auth/login")
    Call<AuthBridgeResponse> login(@Body LoginRequest request);

    // Phase 3: Wallet APIs
    @GET("wallet")
    Call<WalletResponse> getWallet(@Query("userId") String userId);

    @POST("wallet/add-money")
    Call<WalletResponse> addMoney(@Query("userId") String userId, @Body AddMoneyRequest body);

    @POST("transfer")
    Call<TransferResponse> transfer(@Body TransferRequest body);

    @GET("users/resolve")
    Call<ResolveUserResponse> resolveUser(@Query("identifier") String identifier);

    @GET("transactions")
    Call<List<TransactionDto>> transactions(@Query("userId") String userId);
}
