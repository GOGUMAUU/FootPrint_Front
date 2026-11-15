package com.example.footprint.ui;

import android.content.Intent;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.footprint.data.model.Challenge;
import com.example.footprint.databinding.ItemAchievableChallengeBinding;

import java.util.ArrayList;
import java.util.List;

public class ChallengeAdapter
        extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {

    private final List<Challenge> challenges = new ArrayList<>();

    public void setChallenges(List<Challenge> newChallenges) {
        challenges.clear();
        challenges.addAll(newChallenges);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        ItemAchievableChallengeBinding binding =
                ItemAchievableChallengeBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ChallengeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ChallengeViewHolder holder, int position) {

        Challenge challenge = challenges.get(position);

        // 도전과제 정보 표시
        holder.binding.tvChallengeTitle.setText(challenge.title);
        holder.binding.tvChallengeInfo.setText(challenge.regionName);

// 현재 위치에서 도전과제까지의 거리 표시
        if (challenge.distanceMeters == Float.MAX_VALUE) {

            // GPS 위치를 확인하지 못한 경우
            holder.binding.tvChallengeDistance.setText("거리 확인 불가");

        } else if (challenge.distanceMeters < 1000) {

            // 1km 미만이면 m 단위
            int meters = Math.round(challenge.distanceMeters);

            holder.binding.tvChallengeDistance.setText(
                    meters + "m"
            );

        } else {

            // 1km 이상이면 km 단위 (소수점 첫째 자리)
            String distanceText = String.format(
                    java.util.Locale.KOREA,
                    "%.1fkm",
                    challenge.distanceMeters / 1000
            );

            holder.binding.tvChallengeDistance.setText(distanceText);
        }


        // 백엔드에서 받은 완료 여부에 따라 진행률 표시
        int progress = challenge.isCompleted ? 100 : 0;

        holder.binding.tvProgressPercent.setText(progress + "%");

        holder.binding.getRoot().post(() -> {
            int totalWidth = holder.binding.getRoot().getWidth();

            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams)
                            holder.binding.viewProgress.getLayoutParams();

            params.width = totalWidth * progress / 100;
            holder.binding.viewProgress.setLayoutParams(params);
        });


        // 도전과제 카드 클릭 시 상세 화면으로 이동
        holder.binding.getRoot().setOnClickListener(v -> {

            // 이미 완료한 도전과제는 상세 화면으로 이동하지 않음
            if (challenge.isCompleted) {
                Toast.makeText(
                        v.getContext(),
                        "이미 완료한 도전과제입니다.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(
                    v.getContext(),
                    ChallengeDetailActivity.class
            );


            // 선택한 도전과제 정보 전달
            intent.putExtra("challengeId", challenge.id);
            intent.putExtra("challengeTitle", challenge.title);
            intent.putExtra("challengeDescription", challenge.description);
            intent.putExtra("challengeRegion", challenge.regionName);

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return challenges.size();
    }

    static class ChallengeViewHolder extends RecyclerView.ViewHolder {

        ItemAchievableChallengeBinding binding;

        ChallengeViewHolder(ItemAchievableChallengeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
