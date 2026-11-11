package com.example.footprint.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.footprint.MainActivity;
import com.example.footprint.databinding.ActivityAuthBinding;
import com.example.footprint.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean signup = this instanceof SignupActivity;
        ActivityAuthBinding binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.showPassword.setOnCheckedChangeListener((button, checked) -> {
            for (android.widget.EditText field : new android.widget.EditText[]{binding.password, binding.confirmation}) {
                int selection = field.getSelectionStart();
                field.setTransformationMethod(checked ? android.text.method.HideReturnsTransformationMethod.getInstance()
                        : android.text.method.PasswordTransformationMethod.getInstance());
                if (selection >= 0) field.setSelection(Math.min(selection, field.length()));
            }
        });
        if (signup && android.os.Build.VERSION.SDK_INT >= 26) {
            binding.password.setAutofillHints("newPassword");
            binding.confirmation.setAutofillHints("newPassword");
            binding.password.setHint("비밀번호 (15~64자)");
        }
        AuthViewModel model = new ViewModelProvider(this).get(AuthViewModel.class);
        binding.title.setText(signup ? "회원가입" : "로그인");
        binding.submit.setText(signup ? "가입하기" : "로그인");
        binding.switchMode.setText(signup ? "로그인으로 돌아가기" : "회원가입");
        for (View view : new View[]{binding.confirmation, binding.nickname, binding.terms, binding.marketing}) view.setVisibility(signup ? View.VISIBLE : View.GONE);
        binding.switchMode.setOnClickListener(v -> { if (signup) finish(); else startActivity(new Intent(this, SignupActivity.class)); });
        binding.submit.setOnClickListener(v -> model.submit(signup, binding.email.getText().toString().trim(), binding.password.getText().toString(),
                binding.confirmation.getText().toString(), binding.nickname.getText().toString().trim(), binding.terms.isChecked(), binding.marketing.isChecked()));
        model.state().observe(this, state -> {
            binding.submit.setEnabled(!state.loading);
            binding.switchMode.setEnabled(!state.loading);
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.error.setText(state.error == null ? "" : state.error);
            if (state.success) {
                model.consumeSuccess();
                if (signup) { Toast.makeText(this, "가입되었습니다. 로그인해 주세요.", Toast.LENGTH_LONG).show(); finish(); }
                else { startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
            }
        });
    }
}
