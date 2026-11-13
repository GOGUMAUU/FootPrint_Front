
package com.example.footprint.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.footprint.data.model.Badge;
import com.example.footprint.databinding.ItemBadgeBinding;

import java.util.ArrayList;
import java.util.List;

public class BadgeAdapter
        extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private final List<Badge> badges = new ArrayList<>();

    // 배지 목록 업데이트
    public void setBadges(List<Badge> newBadges) {
        badges.clear();

        if (newBadges != null) {
            badges.addAll(newBadges);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemBadgeBinding binding = ItemBadgeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new BadgeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BadgeViewHolder holder,
            int position
    ) {
        Badge badge = badges.get(position);

        // 배지 이름 표시
        holder.binding.tvBadgeName.setText(
                badge.name != null ? badge.name : "이름 없는 배지"
        );

        // 획득 날짜 표시
        holder.binding.tvBadgeDate.setText(
                badge.earnedAt != null ? badge.earnedAt : ""
        );

        // 임시 기본 배지 아이콘
        holder.binding.ivBadgeIcon.setImageResource(
                android.R.drawable.btn_star_big_on
        );
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class BadgeViewHolder
            extends RecyclerView.ViewHolder {

        final ItemBadgeBinding binding;

        BadgeViewHolder(ItemBadgeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
