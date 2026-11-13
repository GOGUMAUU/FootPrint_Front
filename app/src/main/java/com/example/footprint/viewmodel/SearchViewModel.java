package com.example.footprint.viewmodel;

import androidx.lifecycle.*;
import com.example.footprint.BuildConfig;
import com.example.footprint.data.model.*;
import com.example.footprint.data.repository.SearchRepository;
import java.util.*;
import retrofit2.Call;

public final class SearchViewModel extends ViewModel {
    public static final class State {
        public final List<UserSummary> users;
        public final String message;
        public final boolean loading, hasNext;
        State(List<UserSummary> users, String message, boolean loading, boolean hasNext) {
            this.users = new ArrayList<>(users); this.message = message; this.loading = loading; this.hasNext = hasNext;
        }
    }
    private final SearchRepository repository = new SearchRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Collections.emptyList(), "검색어를 입력해 주세요.", false, false));
    private final List<UserSummary> rows = new ArrayList<>();
    private Call<UserPage> pending;
    private int generation, nextPage;
    private String query = "", type = "NICKNAME";
    public LiveData<State> state() { return state; }
    public boolean isMock() { return BuildConfig.DEBUG && BuildConfig.API_BASE_URL.equals("https://example.com/"); }
    public void search(String query, String type) {
        generation++; if (pending != null) pending.cancel();
        this.query = query.trim(); this.type = type; nextPage = 0; rows.clear();
        if (this.query.isEmpty()) { state.setValue(new State(rows, "검색어를 입력해 주세요.", false, false)); return; }
        request();
    }
    public void more() { if (!state.getValue().loading && state.getValue().hasNext) request(); }
    private void request() {
        int requestId = generation;
        state.setValue(new State(rows, "검색 중…", true, false));
        pending = repository.search(query, type, nextPage, isMock(), (page, error) -> {
            if (requestId != generation) return;
            if (error != null) { state.setValue(new State(rows, error, false, nextPage > 0)); return; }
            if (page.content != null) rows.addAll(page.content);
            nextPage = page.page + 1;
            state.setValue(new State(rows, rows.isEmpty() ? "검색 결과가 없습니다." : rows.size() + "명 검색됨", false, page.hasNext));
        });
    }
    @Override protected void onCleared() { generation++; if (pending != null) pending.cancel(); }
}
