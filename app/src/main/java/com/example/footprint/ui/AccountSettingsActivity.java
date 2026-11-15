package com.example.footprint.ui;

import android.os.Bundle;
import androidx.appcompat.app.*;
import androidx.lifecycle.ViewModelProvider;
import com.example.footprint.databinding.ActivityAccountBinding;
import com.example.footprint.security.Session;
import com.example.footprint.viewmodel.AccountViewModel;

public final class AccountSettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Session.signedIn(this)) { Session.login(this); return; }
        ActivityAccountBinding binding = ActivityAccountBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        AccountViewModel model = new ViewModelProvider(this).get(AccountViewModel.class);
        binding.changePassword.setOnClickListener(v -> model.password(binding.currentPassword.getText().toString(), binding.newPassword.getText().toString(), binding.confirmPassword.getText().toString()));
        binding.logout.setOnClickListener(v -> Session.logout(this));
        binding.deleteAccount.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("회원 탈퇴")
                .setMessage("계정 탈퇴를 요청하고 모든 세션에서 로그아웃합니다. 데이터 보존·재인증 정책은 서버 확정이 필요합니다. 계속할까요?")
                .setNegativeButton("취소", null).setPositiveButton("탈퇴", (d,w) -> model.delete()).show());
        model.error.observe(this, binding.error::setText);
        model.busy.observe(this, busy -> { binding.changePassword.setEnabled(!busy); binding.deleteAccount.setEnabled(!busy); binding.logout.setEnabled(!busy); });
        model.completed.observe(this, completed -> { if (completed) { model.completed.setValue(false); Session.clear(this); Session.login(this); } });
    }
}
