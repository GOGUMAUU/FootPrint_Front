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
    private ActivityUserSearchBinding binding;
    private Call<List<UserSummary>> pending;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.btnSearch.setOnClickListener(v -> search());
    }
    private void search() {
        String q = binding.etSearch.getText().toString().trim();
        if (q.isEmpty()) return;
        if (pending != null) pending.cancel();
        binding.btnSearch.setEnabled(false);
        binding.tvPolicy.setText("검색 중…");
        pending = ApiClient.get().searchUsers(q);
        pending.enqueue(new Callback<List<UserSummary>>() {
            @Override public void onResponse(@NonNull Call<List<UserSummary>> call, @NonNull Response<List<UserSummary>> response) {
                if (isDestroyed() || call != pending) return;
                binding.btnSearch.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    binding.tvPolicy.setText("검색에 실패했습니다. 다시 시도해 주세요.");
                    return;
                }
                java.util.List<UserSummary> users = response.body();
                binding.tvPolicy.setText(users.isEmpty() ? "검색 결과가 없습니다." : users.size() + "명 검색됨");
                binding.recyclerUsers.setAdapter(new UserRows(users));
            }
            @Override public void onFailure(@NonNull Call<List<UserSummary>> call, @NonNull Throwable t) {
                if (isDestroyed() || call.isCanceled() || call != pending) return;
                binding.btnSearch.setEnabled(true);
                binding.tvPolicy.setText("네트워크 연결을 확인하고 다시 검색해 주세요.");
            }
        });
    }
    @Override protected void onDestroy() { if (pending != null) pending.cancel(); super.onDestroy(); }
    private static final class UserRows extends androidx.recyclerview.widget.RecyclerView.Adapter<UserRows.Holder> {
        private final List<UserSummary> users;
        UserRows(List<UserSummary> users) { this.users = users; }
        static final class Holder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            Holder(android.widget.TextView view) { super(view); }
        }
        @Override public Holder onCreateViewHolder(android.view.ViewGroup parent, int type) {
            android.widget.TextView view = new android.widget.TextView(parent.getContext());
            view.setLayoutParams(new androidx.recyclerview.widget.RecyclerView.LayoutParams(-1, -2));
            int padding = (int) (16 * parent.getResources().getDisplayMetrics().density);
            view.setPadding(padding, padding, padding, padding);
            return new Holder(view);
        }
        @Override public void onBindViewHolder(Holder holder, int position) {
            UserSummary user = users.get(position);
            ((android.widget.TextView) holder.itemView).setText(user.nickname + "\n" + user.userCode);
        }
        @Override public int getItemCount() { return users.size(); }
    }
}
