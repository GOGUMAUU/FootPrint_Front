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
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUserSearchBinding binding = ActivityUserSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        com.example.footprint.viewmodel.SearchViewModel model = new androidx.lifecycle.ViewModelProvider(this).get(com.example.footprint.viewmodel.SearchViewModel.class);
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.searchType.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"닉네임", "친구 코드"}));
        Runnable search = () -> model.search(binding.etSearch.getText().toString(), binding.searchType.getSelectedItemPosition() == 0 ? "NICKNAME" : "USER_CODE");
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
        binding.searchType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) { handler.removeCallbacksAndMessages(null); search.run(); }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        model.state().observe(this, state -> {
            binding.recyclerUsers.setAdapter(new UserRows(state.users));
            binding.btnSearch.setEnabled(!state.loading);
            binding.btnMore.setEnabled(!state.loading && state.hasNext);
            binding.btnMore.setVisibility(state.hasNext ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.tvPolicy.setText((model.isMock() ? "개발용 Mock · 산책친구 또는 FP00000001로 검색\n" : "") + state.message);
        });
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
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
            ((android.widget.TextView) holder.itemView).setText(user.nickname + "\n" + user.friendCode);
            holder.itemView.setOnClickListener(v -> {
                if (user.userId != null) v.getContext().startActivity(new android.content.Intent(v.getContext(), UserProfileActivity.class).putExtra("userId", user.userId.longValue()));
            });
        }
        @Override public int getItemCount() { return users.size(); }
    }
}
