package com.arpit.myapplication;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class UserViewModel extends AndroidViewModel {

    private final UserRepository userRepo;
    private final MutableLiveData<String> _currentUser = new MutableLiveData<>();
    public final LiveData<String> currentUser = _currentUser;

    private final MutableLiveData<Boolean> _loginResult = new MutableLiveData<>();
    public final LiveData<Boolean> loginResult = _loginResult;

    private final MutableLiveData<Boolean> _registerResult = new MutableLiveData<>();
    public final LiveData<Boolean> registerResult = _registerResult;

    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepo = new UserRepository(application);
        _currentUser.setValue(userRepo.getCurrentUser());
    }

    public void register(String username, String phone) {
        if (userRepo.exists(username)) {
            _registerResult.setValue(false);
            return;
        }
        userRepo.addUser(username, phone);
        userRepo.setCurrentUser(username);
        _currentUser.setValue(username);
        _registerResult.setValue(true);
    }

    public void login(String username) {
        if (userRepo.exists(username)) {
            userRepo.setCurrentUser(username);
            _currentUser.setValue(username);
            _loginResult.setValue(true);
        } else {
            _loginResult.setValue(false);
        }
    }

    public String getCurrentUser() {
        return userRepo.getCurrentUser();
    }

    public boolean exists(String username) {
        return userRepo.exists(username);
    }

    public String findUserByPhone(String phone) {
        return userRepo.findUserByPhone(phone);
    }
}
