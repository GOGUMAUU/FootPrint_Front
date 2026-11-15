package com.example.footprint.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.footprint.data.model.Badge;
import com.example.footprint.databinding.ActivityMyBadgesBinding;

import java.util.ArrayList;
import java.util.List;

import com.example.footprint.api.ApiClient;
import com.example.footprint.api.ApiService;
import com.example.footprint.data.model.BadgeListResponse;

import android.util.Log;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MyBadgesActivity extends AppCompatActivity {

    private ActivityMyBadgesBinding binding;
    private BadgeAdapter badgeAdapter;

    // 현재 획득한 배지 목록
    private final List<Badge> earnedBadges = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMyBadgesBinding.inflate(
                getLayoutInflater()
        );
        setContentView(binding.getRoot());

        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> finish());

        // 배지 목록 어댑터 생성
        badgeAdapter = new BadgeAdapter();

        // 배지를 2열 격자로 표시
        binding.rvMyBadges.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        binding.rvMyBadges.setAdapter(badgeAdapter);

        // 서버에서 획득한 배지 목록 불러오기
        loadMyBadges();

    }

    // 서버에서 현재 사용자의 배지 목록 조회
    private void loadMyBadges() {

        ApiService apiService = ApiClient.get();

        apiService.getMyBadges().enqueue(
                new Callback<BadgeListResponse>() {

                    @Override
                    public void onResponse(
                            Call<BadgeListResponse> call,
                            Response<BadgeListResponse> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            earnedBadges.clear();

                            // 서버에서 받은 배지를 Android Badge 객체로 변환
                            if (response.body().badges != null) {

                                for (BadgeListResponse.BadgeItem item
                                        : response.body().badges) {

                                    Badge badge = new Badge();

                                    badge.id = String.valueOf(item.questId);
                                    badge.name = item.badgeName;
                                    badge.iconUrl = item.badgeImageUrl;
                                    badge.earnedAt = item.earnedAt;

                                    earnedBadges.add(badge);
                                }
                            }

                            // 배지 목록과 획득 개수 화면 갱신
                            updateBadgeList();

                            Log.d("BadgeAPI",
                                    "획득한 배지 수: " + earnedBadges.size());

                        } else {

                            Log.e("BadgeAPI",
                                    "배지 조회 실패: " + response.code());

                            Toast.makeText(
                                    MyBadgesActivity.this,
                                    "배지 목록을 불러오지 못했습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<BadgeListResponse> call,
                            Throwable t
                    ) {

                        Log.e("BadgeAPI", "서버 연결 실패", t);

                        Toast.makeText(
                                MyBadgesActivity.this,
                                "서버에 연결할 수 없습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // 배지 목록과 빈 상태 화면 업데이트
    private void updateBadgeList() {

        badgeAdapter.setBadges(earnedBadges);

        // 획득한 배지 개수
        binding.tvBadgeCount.setText(
                String.valueOf(earnedBadges.size())
        );

        if (earnedBadges.isEmpty()) {

            // 배지가 없으면 안내 문구 표시
            binding.rvMyBadges.setVisibility(View.GONE);
            binding.layoutEmptyBadges.setVisibility(View.VISIBLE);

        } else {

            // 배지가 있으면 목록 표시
            binding.rvMyBadges.setVisibility(View.VISIBLE);
            binding.layoutEmptyBadges.setVisibility(View.GONE);
        }
    }
}
