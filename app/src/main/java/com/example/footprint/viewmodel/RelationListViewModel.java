package com.example.footprint.viewmodel;

import androidx.lifecycle.*;
import com.example.footprint.api.*;
import com.example.footprint.data.repository.*;
import com.google.gson.*;
import java.util.*;
import retrofit2.*;

public final class RelationListViewModel extends ViewModel {
    public static final class Row { public long userId, relationId; public String nickname; }
    public final MutableLiveData<List<Row>> rows = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false), more = new MutableLiveData<>(false);
    public final MutableLiveData<String> error = new MutableLiveData<>("");
    private final List<Row> all = new ArrayList<>();
    private String type = "requests";
    private long userId;
    private int page;
    private Call<?> pending;
    public void load(String type, long userId, boolean append) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        this.type = type; this.userId = userId;
        if (!append) { page = 0; all.clear(); }
        busy.setValue(true); error.setValue("");
        if (ProfileRepository.mock()) {
            for (Long id : MockRelations.list(type)) { Row row = new Row(); row.userId = id; row.relationId = id; row.nickname = "산책친구 " + id; all.add(row); }
            rows.setValue(new ArrayList<>(all)); more.setValue(false); busy.setValue(false); return;
        }
        RelationshipApi api = ApiClient.create(RelationshipApi.class);
        Call<JsonObject> call = type.equals("requests") ? api.requests(page,20) : type.equals("blocks") ? api.blocks(page,20) : type.equals("followers") ? api.followers(userId,page,20) : api.following(userId,page,20);
        pending = call;
        call.enqueue(new Callback<JsonObject>() {
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                busy.setValue(false);
                try {
                    if (!r.isSuccessful() || r.body() == null) throw new IllegalStateException();
                    List<Row> loaded = new ArrayList<>();
                    for (JsonElement item : r.body().getAsJsonArray("content")) {
                        JsonObject outer = item.getAsJsonObject();
                        JsonObject user = type.equals("requests") ? outer.getAsJsonObject("requester") : type.equals("blocks") ? outer.getAsJsonObject("user") : outer;
                        Row row = new Row(); row.userId = user.get("userId").getAsLong(); row.nickname = user.get("nickname").getAsString();
                        row.relationId = type.equals("requests") ? outer.get("followId").getAsLong() : row.userId; loaded.add(row);
                    }
                    all.addAll(loaded); page++; rows.setValue(new ArrayList<>(all)); more.setValue(r.body().get("hasNext").getAsBoolean());
                } catch (RuntimeException e) { error.setValue("목록을 불러오지 못했습니다. 다시 시도해 주세요."); }
            }
            public void onFailure(Call<JsonObject> c, Throwable t) { if (!c.isCanceled()) { busy.setValue(false); error.setValue("연결을 확인해 주세요."); } }
        });
    }
    public void act(Row row, boolean accept) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        if (ProfileRepository.mock()) {
            if (type.equals("requests")) { if (accept) MockRelations.accept(row.userId); else MockRelations.reject(row.userId); }
            else if (type.equals("blocks")) MockRelations.unblock(row.userId); else MockRelations.unfollow(row.userId);
            load(type,userId,false); return;
        }
        RelationshipApi api = ApiClient.create(RelationshipApi.class);
        Call<?> call = type.equals("requests") ? (accept ? api.accept(row.relationId) : api.reject(row.relationId)) : type.equals("blocks") ? api.unblock(row.userId) : api.unfollow(row.userId);
        perform(call);
    }
    private <T> void perform(Call<T> call) {
        busy.setValue(true); pending = call;
        call.enqueue(new Callback<T>() {
            public void onResponse(Call<T> c, Response<T> r) { busy.setValue(false); if (r.isSuccessful()) load(type,userId,false); else error.setValue("처리 실패 (" + r.code() + ")"); }
            public void onFailure(Call<T> c, Throwable t) { if (!c.isCanceled()) { busy.setValue(false); error.setValue("결과를 확인하지 못했습니다. 새로고침해 주세요."); } }
        });
    }
    @Override protected void onCleared() { if (pending != null) pending.cancel(); }
}
