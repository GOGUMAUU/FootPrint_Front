package com.example.footprint.data.repository;

import com.example.footprint.BuildConfig;
import com.example.footprint.api.*;
import com.example.footprint.data.model.UserProfile;
import com.google.gson.*;
import retrofit2.*;

public final class ProfileRepository {
    public interface Result { void complete(UserProfile profile, String error); }
    private static final java.util.Map<String, UserProfile> accounts = new java.util.HashMap<>();
    private static String account = "demo@example.com";
    public static void select(String email) { account = email; }
    public static void register(String email, String nickname) { UserProfile p = initial(0); p.email = email; p.nickname = nickname; accounts.put(email,p); }
    public static void delete(String email) { accounts.remove(email); }
    private static UserProfile me() { return accounts.computeIfAbsent(account, key -> { UserProfile p = initial(0); p.email = key; return p; }); }
    public static boolean mock() { return BuildConfig.DEBUG && "https://example.com/".equals(BuildConfig.API_BASE_URL); }
    private static UserProfile initial(long id) {
        UserProfile user = new UserProfile(); user.userId = id; user.isSelf = id == 0;
        user.nickname = id == 0 ? "나의 발자국" : String.format(java.util.Locale.ROOT, "산책친구 %02d", id);
        user.friendCode = String.format(java.util.Locale.ROOT, "FP%08d", id);
        user.email = id == 0 ? "demo@example.com" : null;
        user.outgoingFollowStatus = "NONE"; user.incomingFollowStatus = "NONE";
        return user;
    }
    public Call<?> load(long id, Result result) {
        if (mock()) { result.complete(copy(id == 0 ? me() : initial(id)), null); return null; }
        UserApi api = ApiClient.create(UserApi.class);
        return enqueue(id == 0 ? api.me() : api.profile(Long.toString(id)), result);
    }
    public Call<?> update(String nickname, boolean isPrivate, Result result) {
        if (mock()) { UserProfile me = me(); me.nickname = nickname; me.isPrivate = isPrivate; result.complete(copy(me), null); return null; }
        JsonObject body = new JsonObject(); body.addProperty("nickname", nickname); body.addProperty("isPrivate", isPrivate);
        return enqueue(ApiClient.create(UserApi.class).update(body), result);
    }
    public void mockImage(String uri, Result result) { UserProfile me = me(); me.profileImageUrl = uri; result.complete(copy(me), null); }
    private static UserProfile copy(UserProfile user) {
        Gson gson = new Gson(); UserProfile copy = gson.fromJson(gson.toJson(user), UserProfile.class);
        if (copy.userId == 0) { copy.followerCount = MockRelations.list("followers").size(); copy.followingCount = MockRelations.list("following").size(); }
        else { copy.isPrivate = copy.userId % 2 == 0; copy.outgoingFollowStatus = MockRelations.status(copy.userId); copy.incomingFollowStatus = MockRelations.requested(copy.userId) ? "PENDING" : "NONE"; copy.blockedByMe = MockRelations.blocked(copy.userId); }
        return copy;
    }
    private Call<?> enqueue(Call<JsonObject> call, Result result) {
        call.enqueue(new Callback<JsonObject>() {
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                if (!r.isSuccessful() || r.body() == null) { result.complete(null, "프로필 요청 실패 (" + r.code() + ")"); return; }
                result.complete(new Gson().fromJson(r.body(), UserProfile.class), null);
            }
            public void onFailure(Call<JsonObject> c, Throwable t) { if (!c.isCanceled()) result.complete(null, "네트워크 연결을 확인해 주세요."); }
        }); return call;
    }
}
