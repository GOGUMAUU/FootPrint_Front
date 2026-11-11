package com.example.footprint.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.footprint.data.repository.AuthRepository;
import com.example.footprint.security.TokenManager;
import com.example.footprint.util.AuthValidation;
import retrofit2.Call;

public final class AuthViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean loading, success;
        public final String error;
        State(boolean loading, boolean success, String error) { this.loading = loading; this.success = success; this.error = error; }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, false, null));
    private final AuthRepository repository;
    private Call<?> pending;
    public AuthViewModel(Application application) { super(application); repository = new AuthRepository(new TokenManager(application)); }
    public LiveData<State> state() { return state; }
    public void submit(boolean signup, String email, String password, String confirmation, String nickname, boolean terms, boolean marketing) {
        if (state.getValue().loading) return;
        String error = AuthValidation.validate(email, password, confirmation, nickname, terms, signup);
        if (error != null) { state.setValue(new State(false, false, error)); return; }
        state.setValue(new State(true, false, null));
        AuthRepository.Result result = message -> state.setValue(new State(false, message == null, message));
        pending = signup ? repository.signup(email, password, nickname, marketing, result) : repository.login(email, password, result);
    }
    public void consumeSuccess() { state.setValue(new State(false, false, null)); }
    @Override protected void onCleared() { if (pending != null) pending.cancel(); }
}
