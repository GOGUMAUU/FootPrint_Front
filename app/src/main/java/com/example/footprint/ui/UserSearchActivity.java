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
        ApiClient.get().searchUsers(q).enqueue(new Callback<List<UserSummary>>() {
            @Override public void onResponse(@NonNull Call<List<UserSummary>> call, @NonNull Response<List<UserSummary>> response) {
                int count = response.body() == null ? 0 : response.body().size();
                Toast.makeText(UserSearchActivity.this, count + "명 검색됨. RecyclerView 어댑터는 디자인 확정 후 연결하세요.", Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(@NonNull Call<List<UserSummary>> call, @NonNull Throwable t) { Toast.makeText(UserSearchActivity.this, "검색 실패", Toast.LENGTH_SHORT).show(); }
        });
    }
}
