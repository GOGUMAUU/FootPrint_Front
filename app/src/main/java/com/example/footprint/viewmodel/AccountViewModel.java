package com.example.footprint.viewmodel;

import androidx.lifecycle.*;
import com.example.footprint.api.*;
import com.example.footprint.data.repository.*;
import com.example.footprint.util.AuthValidation;
import com.google.gson.JsonObject;
import retrofit2.*;

public final class AccountViewModel extends ViewModel {
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false), completed = new MutableLiveData<>(false);
    public final MutableLiveData<String> error = new MutableLiveData<>("");
    private Call<Void> pending;
    public void password(String current, String next, String confirmation) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        String message = AuthValidation.validateNewPassword(next);
        if (current.isEmpty()) message = "현재 비밀번호를 입력해 주세요.";
        else if (!next.equals(confirmation)) message = "새 비밀번호 확인이 일치하지 않습니다.";
        if (message != null) { error.setValue(message); return; }
        if (ProfileRepository.mock()) {
            message = AuthRepository.changeMockPassword(current, next);
            error.setValue(message == null ? "" : message); completed.setValue(message == null); return;
        }
        JsonObject body = new JsonObject(); body.addProperty("currentPassword", current); body.addProperty("newPassword", next);
        execute(ApiClient.create(UserApi.class).password(body));
    }
    public void delete() {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        if (ProfileRepository.mock()) { AuthRepository.deleteMockAccount(); completed.setValue(true); return; }
        execute(ApiClient.create(UserApi.class).deleteAccount());
    }
    private void execute(Call<Void> call) {
        pending = call; busy.setValue(true); error.setValue("");
        call.enqueue(new Callback<Void>() {
            public void onResponse(Call<Void> c, Response<Void> r) {
                busy.setValue(false);
                if (r.isSuccessful()) completed.setValue(true); else error.setValue("계정 변경 실패 (" + r.code() + "). 입력과 인증 상태를 확인해 주세요.");
            }
            public void onFailure(Call<Void> c, Throwable t) { if (!c.isCanceled()) { busy.setValue(false); error.setValue("처리 결과를 확인하지 못했습니다. 재로그인으로 계정 상태를 확인해 주세요."); } }
        });
    }
    @Override protected void onCleared() { if (pending != null) pending.cancel(); }
}
