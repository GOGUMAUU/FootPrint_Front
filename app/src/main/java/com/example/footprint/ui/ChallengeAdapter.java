package com.example.footprint.ui;

import android.content.Intent;
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

        // 진행률은 API 연결 전까지 0%
        int progress = 0;
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
