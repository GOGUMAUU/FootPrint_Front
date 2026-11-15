package com.example.footprint.data.repository;

import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.UserPage;
import com.example.footprint.data.model.UserSummary;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import retrofit2.*;

public final class SearchRepository {
    public interface Result { void complete(UserPage page, String error); }
    public Call<UserPage> search(String query, String type, int page, boolean mock, Result result) {
        if (mock) { result.complete(mockPage(query, type, page, 20), null); return null; }
        Call<UserPage> call = ApiClient.get().searchUsers(query, type, page, 20);
        call.enqueue(new Callback<UserPage>() {
            public void onResponse(Call<UserPage> c, Response<UserPage> r) {
                result.complete(r.body(), r.isSuccessful() && r.body() != null ? null : "검색에 실패했습니다. 다시 시도해 주세요.");
            }
            public void onFailure(Call<UserPage> c, Throwable t) {
                if (!c.isCanceled()) result.complete(null, "네트워크 연결을 확인해 주세요.");
            }
        });
        return call;
    }
    public static UserPage mockPage(String query, String type, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page");
        List<UserSummary> matches = new ArrayList<>();
        String q = query.trim();
        for (int i = 1; i <= 45; i++) {
            if (MockRelations.blocked(i)) continue;
            UserSummary user = new UserSummary();
            user.userId = (long) i;
            user.nickname = String.format(Locale.ROOT, "산책친구 %02d", i);
            user.friendCode = String.format(Locale.ROOT, "FP%08d", i);
            user.following = "ACCEPTED".equals(MockRelations.status(i));
            if (!q.isEmpty() && ("USER_CODE".equals(type)
                    ? user.friendCode.equals(q.toUpperCase(Locale.ROOT)) : user.nickname.contains(q))) matches.add(user);
        }
        UserPage result = new UserPage(); result.page = page; result.size = size;
        int start = (int) Math.min((long) page * size, matches.size());
        int end = Math.min(start + size, matches.size());
        result.content.addAll(matches.subList(start, end)); result.hasNext = end < matches.size();
        return result;
    }
}
