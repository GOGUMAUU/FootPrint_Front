package com.example.footprint.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.footprint.R;
import com.example.footprint.api.ApiClient;
import com.example.footprint.data.model.MapFilterState;
import com.example.footprint.data.model.UserSummary;
import com.example.footprint.databinding.DialogMapFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilterBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(MapFilterState filterState);
    }

    private DialogMapFilterBinding binding;
    private MapFilterState currentState;
    private OnFilterAppliedListener listener;

    public static FilterBottomSheetDialog newInstance(MapFilterState state) {
        return newInstance(state, "nearby");
    }

    public static FilterBottomSheetDialog newInstance(MapFilterState state, String currentMode) {
        FilterBottomSheetDialog fragment = new FilterBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable("filterState", state);
        args.putString("MODE", currentMode);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogMapFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null && getArguments().getSerializable("filterState") != null) {
            currentState = (MapFilterState) getArguments().getSerializable("filterState");
        } else {
            currentState = new MapFilterState();
        }

        restoreChipSelection();

        // onViewCreated 내부에서 모드 확인 후 팔로잉 칩 로드
        String currentMode = getArguments() != null ? getArguments().getString("MODE", "nearby") : "nearby";
        if ("following".equals(currentMode)) {
            binding.layoutFollowingFilter.setVisibility(View.VISIBLE);
            loadFollowingChips();
        } else {
            binding.layoutFollowingFilter.setVisibility(View.GONE);
        }

        binding.btnResetFilter.setOnClickListener(v -> {
            currentState.reset();
            restoreChipSelection();
            if ("following".equals(currentMode)) {
                loadFollowingChips();
            }
        });

        binding.btnApplyFilter.setOnClickListener(v -> {
            saveChipSelection();
            if (listener != null) {
                listener.onFilterApplied(currentState);
            }
            dismiss();
        });
    }

    private void restoreChipSelection() {
        if ("popular".equals(currentState.getSort())) {
            binding.chipSortPopular.setChecked(true);
        } else {
            binding.chipSortLatest.setChecked(true);
        }

        if ("7d".equals(currentState.getPeriod())) {
            binding.chipPeriod7d.setChecked(true);
        } else if ("30d".equals(currentState.getPeriod())) {
            binding.chipPeriod30d.setChecked(true);
        } else {
            binding.chipPeriodAll.setChecked(true);
        }

        if ("10s".equals(currentState.getAgeGroup())) {
            binding.chipAge10s.setChecked(true);
        } else if ("20s".equals(currentState.getAgeGroup())) {
            binding.chipAge20s.setChecked(true);
        } else if ("30s".equals(currentState.getAgeGroup())) {
            binding.chipAge30s.setChecked(true);
        } else {
            binding.chipAgeAll.setChecked(true);
        }
    }

    private void loadFollowingChips() {
        binding.chipGroupFollowing.removeAllViews();

        Chip allChip = createUserChip("전체", null);
        binding.chipGroupFollowing.addView(allChip);
        if (currentState.getSelectedUserId() == null) {
            allChip.setChecked(true);
        }

        ApiClient.get().getMyProfile().enqueue(new Callback<UserSummary>() {
            @Override
            public void onResponse(@NonNull Call<UserSummary> call, @NonNull Response<UserSummary> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null || response.body().userId == null) return;

                long myUserId = response.body().userId;

                // 2. 내 팔로잉 목록 조회 (/api/users/{userId}/following)
                ApiClient.get().getUserFollowing(myUserId).enqueue(new Callback<List<UserSummary>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<UserSummary>> call2,
                                           @NonNull Response<List<UserSummary>> response2) {
                        if (!isAdded() || !response2.isSuccessful() || response2.body() == null) return;

                        for (UserSummary user : response2.body()) {
                            if (user == null || user.userId == null) continue;
                            Chip userChip = createUserChip(user.nickname, user.userId);
                            binding.chipGroupFollowing.addView(userChip);

                            if (user.userId.equals(currentState.getSelectedUserId())) {
                                userChip.setChecked(true);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<UserSummary>> call2, @NonNull Throwable t) { }
                });
            }

            @Override
            public void onFailure(@NonNull Call<UserSummary> call, @NonNull Throwable t) { }
        });
    }

    private Chip createUserChip(String label, @Nullable Long userId) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentState.setSelectedUserId(userId);
            }
        });
        return chip;
    }

    private void saveChipSelection() {
        int sortId = binding.chipGroupSort.getCheckedChipId();
        currentState.setSort(sortId == R.id.chipSortPopular ? "popular" : "latest");

        int periodId = binding.chipGroupPeriod.getCheckedChipId();
        if (periodId == R.id.chipPeriod7d) currentState.setPeriod("7d");
        else if (periodId == R.id.chipPeriod30d) currentState.setPeriod("30d");
        else currentState.setPeriod(null);

        int ageId = binding.chipGroupAge.getCheckedChipId();
        if (ageId == R.id.chipAge10s) currentState.setAgeGroup("10s");
        else if (ageId == R.id.chipAge20s) currentState.setAgeGroup("20s");
        else if (ageId == R.id.chipAge30s) currentState.setAgeGroup("30s");
        else currentState.setAgeGroup(null);
    }
}