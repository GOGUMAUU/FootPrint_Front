package com.example.footprint.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.footprint.R;
import com.example.footprint.data.model.MapFilterState;
import com.example.footprint.databinding.DialogMapFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FilterBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(MapFilterState filterState);
    }

    private DialogMapFilterBinding binding;
    private MapFilterState currentState;
    private OnFilterAppliedListener listener;

    public static FilterBottomSheetDialog newInstance(MapFilterState state) {
        FilterBottomSheetDialog fragment = new FilterBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable("filterState", state);
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

        binding.btnResetFilter.setOnClickListener(v -> {
            currentState.reset();
            restoreChipSelection();
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
        // 정렬 복원
        if ("popular".equals(currentState.getSort())) {
            binding.chipSortPopular.setChecked(true);
        } else {
            binding.chipSortLatest.setChecked(true);
        }

        // 기간 복원
        if ("7d".equals(currentState.getPeriod())) {
            binding.chipPeriod7d.setChecked(true);
        } else if ("30d".equals(currentState.getPeriod())) {
            binding.chipPeriod30d.setChecked(true);
        } else {
            binding.chipPeriodAll.setChecked(true);
        }

        // 연령대 복원
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