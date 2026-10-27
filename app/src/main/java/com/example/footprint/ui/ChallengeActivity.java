package com.example.footprint.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.footprint.databinding.ActivityChallengeBinding;

public class ChallengeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityChallengeBinding binding = ActivityChallengeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnVerify.setOnClickListener(v -> Toast.makeText(this, "현재 GPS → verify-location API", Toast.LENGTH_SHORT).show());
        binding.btnSubmit.setOnClickListener(v -> Toast.makeText(this, "사진/기록 → submissions API", Toast.LENGTH_SHORT).show());
    }
}
