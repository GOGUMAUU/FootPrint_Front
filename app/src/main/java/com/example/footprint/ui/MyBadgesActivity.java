package com.example.footprint.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.footprint.data.model.Badge;
import com.example.footprint.databinding.ActivityMyBadgesBinding;

import java.util.ArrayList;
import java.util.List;

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

        // 배지 목록 업데이트
        updateBadgeList();

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
