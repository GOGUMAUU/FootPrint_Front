package com.example.footprint.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.footprint.R;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.PageResponse;
import com.example.footprint.data.model.UserSummary;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 💌 편지 받는 친구 고르기.
 * 팔로잉 목록을 100명씩 최대 5페이지까지 받아 두고, 검색창으로 닉네임·친구 코드를 바로 거른다.
 * 고르면 FragmentResult(RESULT_KEY)로 userId / nickname 을 돌려준다.
 */
public class RecipientPickerSheet extends BottomSheetDialogFragment {

    public static final String RESULT_KEY = "letter_recipient";
    public static final String RESULT_ID = "userId";
    public static final String RESULT_NAME = "nickname";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 5;

    public static void show(FragmentManager fm) {
        if (fm.isStateSaved()) return;
        new RecipientPickerSheet().show(fm, "recipient_picker");
    }

    private final List<UserSummary> all = new ArrayList<>();
    private final Adapter adapter = new Adapter();
    private TextView state;
    private EditText search;
    private long myId;
    @Nullable private Call<?> pending;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        return inflater.inflate(R.layout.sheet_recipient_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        state = v.findViewById(R.id.tvRecipientState);
        search = v.findViewById(R.id.etRecipientSearch);
        RecyclerView rv = v.findViewById(R.id.rvRecipients);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) { applyFilter(); }
        });
        loadMe();
    }

    @Override
    public void onStart() {
        super.onStart();
        View sheet = getDialog() == null ? null : getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.setBackgroundColor(Color.TRANSPARENT);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.6));
            behavior.setMaxHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.85));
        }
    }

    @Override
    public void onDestroyView() {
        if (pending != null) pending.cancel();
        super.onDestroyView();
    }

    private void loadMe() {
        showState("친구 목록을 불러오는 중이에요…");
        Call<UserSummary> call = ApiClient.get().getMyProfile();
        pending = call;
        call.enqueue(new Callback<UserSummary>() {
            @Override public void onResponse(@NonNull Call<UserSummary> c, @NonNull Response<UserSummary> r) {
                if (!isAdded() || c.isCanceled()) return;
                if (!r.isSuccessful() || r.body() == null || r.body().userId == null) { showState("친구 목록을 불러오지 못했어요."); return; }
                myId = r.body().userId;
                loadPage(0);
            }
            @Override public void onFailure(@NonNull Call<UserSummary> c, @NonNull Throwable t) {
                if (isAdded() && !c.isCanceled()) showState("서버에 연결할 수 없어요.");
            }
        });
    }

    private void loadPage(int page) {
        Call<PageResponse<UserSummary>> call = ApiClient.get().getUserFollowing(myId, page, PAGE_SIZE);
        pending = call;
        call.enqueue(new Callback<PageResponse<UserSummary>>() {
            @Override public void onResponse(@NonNull Call<PageResponse<UserSummary>> c, @NonNull Response<PageResponse<UserSummary>> r) {
                if (!isAdded() || c.isCanceled()) return;
                if (!r.isSuccessful() || r.body() == null) { if (all.isEmpty()) showState("친구 목록을 불러오지 못했어요."); return; }
                if (r.body().content != null) all.addAll(r.body().content);
                applyFilter();
                if (r.body().hasNext && page + 1 < MAX_PAGES) loadPage(page + 1);
            }
            @Override public void onFailure(@NonNull Call<PageResponse<UserSummary>> c, @NonNull Throwable t) {
                if (isAdded() && !c.isCanceled() && all.isEmpty()) showState("서버에 연결할 수 없어요.");
            }
        });
    }

    private void applyFilter() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<UserSummary> shown = new ArrayList<>();
        for (UserSummary u : all) {
            if (u.userId == null) continue;
            String name = u.nickname == null ? "" : u.nickname.toLowerCase(Locale.ROOT);
            String code = u.friendCode == null ? "" : u.friendCode.toLowerCase(Locale.ROOT);
            if (q.isEmpty() || name.contains(q) || code.contains(q)) shown.add(u);
        }
        adapter.submit(shown);
        if (all.isEmpty()) showState("아직 팔로우 중인 친구가 없어요.\n친구를 팔로우하면 편지를 보낼 수 있어요.");
        else if (shown.isEmpty()) showState("'" + search.getText().toString().trim() + "' 친구를 찾지 못했어요.");
        else state.setVisibility(View.GONE);
    }

    private void showState(String msg) {
        state.setText(msg);
        state.setVisibility(View.VISIBLE);
    }

    private void pick(UserSummary u) {
        Bundle result = new Bundle();
        result.putLong(RESULT_ID, u.userId);
        result.putString(RESULT_NAME, u.nickname);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final List<UserSummary> items = new ArrayList<>();

        void submit(List<UserSummary> list) { items.clear(); items.addAll(list); notifyDataSetChanged(); }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipient, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            UserSummary u = items.get(position);
            h.name.setText(u.nickname == null ? "친구" : u.nickname);
            h.code.setText(u.friendCode == null ? "" : "#" + u.friendCode);
            h.itemView.setOnClickListener(x -> pick(u));
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name, code;
            Holder(View v) { super(v); name = v.findViewById(R.id.tvRecipientName); code = v.findViewById(R.id.tvRecipientCode); }
        }
    }
}
