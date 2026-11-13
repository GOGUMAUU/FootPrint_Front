package com.example.footprint.viewmodel;

import androidx.lifecycle.*;
import com.example.footprint.api.*;
import com.example.footprint.data.model.Policy;
import com.example.footprint.data.repository.ProfileRepository;
import com.google.gson.*;
import java.util.*;
import retrofit2.*;

public final class PolicyViewModel extends ViewModel {
    public final MutableLiveData<List<Policy>> policies = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<String> error = new MutableLiveData<>("");
    private Call<JsonElement> pending;
    public void load() {
        if (pending != null) pending.cancel();
        if (ProfileRepository.mock()) {
            List<Policy> list = new ArrayList<>();
            for (String type : new String[]{"TERMS", "PRIVACY", "LOCATION"}) {
                Policy p = new Policy(); p.type = type; p.version = "mock-v1";
                p.content = "개발용 예시 · 실제 약관 미확정\n\n" + type + " 화면과 동의 흐름 검증용입니다. 실제 서비스 동의로 저장하지 않습니다.";
                p.consentRequired = !type.equals("PRIVACY"); p.withdrawable = type.equals("LOCATION"); list.add(p);
            }
            policies.setValue(list); return;
        }
        pending = ApiClient.create(PolicyApi.class).policies();
        pending.enqueue(new Callback<JsonElement>() {
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                try {
                    if (!r.isSuccessful() || r.body() == null) throw new IllegalStateException();
                    Policy[] values = new Gson().fromJson(r.body().getAsJsonObject().get("policies"), Policy[].class);
                    if (values == null) throw new IllegalStateException();
                    policies.setValue(Arrays.asList(values)); error.setValue("");
                } catch (RuntimeException e) { error.setValue("게시된 약관을 불러오지 못했습니다. 다시 시도해 주세요."); }
            }
            public void onFailure(Call<JsonElement> c, Throwable t) { if (!c.isCanceled()) error.setValue("약관 조회 실패. 연결을 확인해 주세요."); }
        });
    }
    public Policy find(String type) {
        for (Policy p : policies.getValue()) if (type.equals(p.type) && p.version != null && p.content != null) return p;
        return null;
    }
    @Override protected void onCleared() { if (pending != null) pending.cancel(); }
}
