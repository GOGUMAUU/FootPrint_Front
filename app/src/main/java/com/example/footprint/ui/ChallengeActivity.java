package com.example.footprint.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.footprint.databinding.ActivityChallengeBinding;

public class ChallengeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.example.footprint.security.Session.signedIn(this)) { com.example.footprint.security.Session.login(this); return; }
        ActivityChallengeBinding binding = ActivityChallengeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnVerify.setOnClickListener(v -> LocationConsent.request(this, () -> Toast.makeText(this, "위치 인증 API 연동은 아직 미구현입니다.", Toast.LENGTH_SHORT).show()));
        binding.btnSubmit.setOnClickListener(v -> Toast.makeText(this, "사진/기록 → submissions API", Toast.LENGTH_SHORT).show());
    }
}
