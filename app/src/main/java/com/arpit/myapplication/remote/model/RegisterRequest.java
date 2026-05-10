package com.arpit.myapplication.remote.model;

public class RegisterRequest {
    private final String username;
    private final String email;
    private final Integer age;
    private final String phone;
    private final String password;
    private final String otp;

    public RegisterRequest(String username, String email, Integer age, String phone, String password, String otp) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.phone = phone;
        this.password = password;
        this.otp = otp;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getOtp() {
        return otp;
    }
}
