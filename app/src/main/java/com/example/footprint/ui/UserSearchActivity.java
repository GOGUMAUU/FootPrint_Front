package com.example.footprint.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.footprint.api.ApiClient;
import com.example.footprint.databinding.ActivityUserSearchBinding;
import com.example.footprint.data.model.UserSummary;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserSearchActivity extends AppCompatActivity {
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable debounce;
    private final java.util.Map<Long, com.example.footprint.viewmodel.UserViewModel> relations = new java.util.HashMap<>();
    private UserRows adapter;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUserSearchBinding binding = ActivityUserSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        com.example.footprint.viewmodel.SearchViewModel model = new androidx.lifecycle.ViewModelProvider(this).get(com.example.footprint.viewmodel.SearchViewModel.class);
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserRows(java.util.Collections.emptyList());
        binding.recyclerUsers.setAdapter(adapter);
        com.example.footprint.viewmodel.UserViewModel mine = new androidx.lifecycle.ViewModelProvider(this).get(com.example.footprint.viewmodel.UserViewModel.class);
        mine.profile.observe(this, user -> binding.myCode.setText("내 친구 코드\n" + (user.friendCode == null ? "—" : user.friendCode)));
        binding.copyCode.setOnClickListener(v -> { if (mine.profile.getValue() != null && mine.profile.getValue().friendCode != null) {
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("친구 코드",mine.profile.getValue().friendCode));
            Toast.makeText(this,"친구 코드를 복사했습니다.",Toast.LENGTH_SHORT).show();
        }});
        binding.requests.setOnClickListener(v -> startActivity(new android.content.Intent(this,RelationListActivity.class).putExtra("type","requests").putExtra("own",true)));
        mine.load(0);

        Runnable search = () -> model.search(binding.etSearch.getText().toString(), null);
        binding.btnSearch.setOnClickListener(v -> { handler.removeCallbacksAndMessages(null); search.run(); });
        binding.btnMore.setOnClickListener(v -> model.more());
        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounce != null) handler.removeCallbacks(debounce);
                debounce = search; handler.postDelayed(debounce, 350);
            }
            public void afterTextChanged(android.text.Editable s) {}
        });
        model.state().observe(this, state -> {
            adapter.users = state.users;
            for (UserSummary user : state.users) {
                if (user.userId == null || relations.containsKey(user.userId)) continue;
                com.example.footprint.viewmodel.UserViewModel relation = new androidx.lifecycle.ViewModelProvider(this)
                        .get("search-user-" + user.userId, com.example.footprint.viewmodel.UserViewModel.class);
                relations.put(user.userId, relation);
                relation.profile.observe(this, profile -> adapter.notifyDataSetChanged());
                relation.busy.observe(this, busy -> adapter.notifyDataSetChanged());
                relation.error.observe(this, error -> {
                    if (error != null && !error.isEmpty()) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                });
                relation.load(user.userId);
            }
            adapter.notifyDataSetChanged();
            binding.btnSearch.setEnabled(!state.loading);
            binding.btnMore.setEnabled(!state.loading && state.hasNext);
            binding.btnMore.setVisibility(state.hasNext ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.tvPolicy.setText((model.isMock() ? "개발용 Mock · 산책친구 또는 FP00000001로 검색\n" : "") + state.message);
        });
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override protected void onResume() {
        super.onResume();
        for (java.util.Map.Entry<Long, com.example.footprint.viewmodel.UserViewModel> entry : relations.entrySet()) entry.getValue().load(entry.getKey());
    }
    private final class UserRows extends androidx.recyclerview.widget.RecyclerView.Adapter<UserRows.Holder> {
        private List<UserSummary> users;
        UserRows(List<UserSummary> users) { this.users = users; }
        final class Holder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            Holder(FriendCard view) { super(view); }
        }
        @Override public Holder onCreateViewHolder(android.view.ViewGroup parent, int type) {
            return new Holder(new FriendCard(parent.getContext()));
        }
        @Override public void onBindViewHolder(Holder holder, int position) {
            UserSummary user = users.get(position);
            FriendCard card = (FriendCard)holder.itemView; card.name.setText(user.nickname); card.subtitle.setText(user.friendCode); card.actions.removeAllViews();
            com.bumptech.glide.Glide.with(card).load(user.profileImageUrl).placeholder(com.example.footprint.R.drawable.ic_basic_profile).error(com.example.footprint.R.drawable.ic_basic_profile).into(card.avatar);
            com.example.footprint.viewmodel.UserViewModel relation = relations.get(user.userId);
            com.example.footprint.data.model.UserProfile profile = relation == null ? null : relation.profile.getValue();
            String status = profile == null ? null : profile.outgoingFollowStatus;
            boolean following = "ACCEPTED".equals(status);
            boolean requested = "PENDING".equals(status);
            if (profile == null || !profile.isSelf) {
                android.widget.Button action = card.action(profile == null ? (relation != null && Boolean.TRUE.equals(relation.busy.getValue()) ? "확인 중" : "재시도") : profile.blockedByMe ? "차단 해제" : requested ? "요청됨" : following ? "팔로잉" : "팔로우", !following && !requested);
                action.setEnabled(relation != null && !Boolean.TRUE.equals(relation.busy.getValue()));
                action.setContentDescription(requested ? "팔로우 요청 취소" : following ? "언팔로우" : action.getText());
                action.setOnClickListener(v -> {
                    if (profile == null) { relation.load(user.userId); return; }
                    String operation = profile.blockedByMe ? "unblock" : following || requested ? "unfollow" : "follow";
                    if ("follow".equals(operation)) { relation.relationship(operation, user.userId); return; }
                    String label = profile.blockedByMe ? "차단 해제" : requested ? "요청 취소" : "언팔로우";
                    new androidx.appcompat.app.AlertDialog.Builder(UserSearchActivity.this)
                            .setMessage(label + "할까요?").setNegativeButton("취소", null)
                            .setPositiveButton(label, (dialog, which) -> relation.relationship(operation, user.userId)).show();
                });
            }
            holder.itemView.setOnClickListener(v -> {
                if (user.userId != null) v.getContext().startActivity(new android.content.Intent(v.getContext(), UserProfileActivity.class).putExtra("userId", user.userId.longValue()));
            });
        }
        @Override public int getItemCount() { return users.size(); }
    }
}
