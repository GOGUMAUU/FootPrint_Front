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
        binding.brand.setVisibility(signup ? View.GONE : View.VISIBLE);
        binding.signupHeader.setVisibility(signup ? View.VISIBLE : View.GONE);
        binding.welcome.setVisibility(signup ? View.VISIBLE : View.GONE);
        binding.back.setOnClickListener(v -> finish());
        binding.showPassword.setOnCheckedChangeListener((button, checked) -> {
            for (android.widget.EditText field : new android.widget.EditText[]{binding.password, binding.confirmation}) {
                int selection = field.getSelectionStart();
                field.setTransformationMethod(checked ? android.text.method.HideReturnsTransformationMethod.getInstance()
                        : android.text.method.PasswordTransformationMethod.getInstance());
                if (selection >= 0) field.setSelection(Math.min(selection, field.length()));
            }
        });
        if (signup) binding.password.setHint("비밀번호 (8~64자)");
        if (signup && android.os.Build.VERSION.SDK_INT >= 26) {
            binding.password.setAutofillHints("newPassword");
            binding.confirmation.setAutofillHints("newPassword");
        }
        AuthViewModel model = new ViewModelProvider(this).get(AuthViewModel.class);
        // 회원가입 때 필수 약관(이용약관 + 위치기반서비스)을 한 번에 받는다.
        // 위치 동의를 여기서 받아 두면 로그인 후 지도에서 다시 묻지 않음 (LocationConsent 가 서버 동의를 확인)
        com.example.footprint.viewmodel.PolicyViewModel policies =
                new ViewModelProvider(this).get(com.example.footprint.viewmodel.PolicyViewModel.class);
        for (View view : new View[]{binding.terms, binding.locationTerms, binding.readTerms})
            view.setVisibility(signup ? View.VISIBLE : View.GONE);
        if (signup) {
            binding.terms.setText("[필수] 이용약관에 동의해요 (개인정보 처리방침 확인)");
            binding.readTerms.setText("약관 전문 보기");
            binding.readTerms.setOnClickListener(v -> showPolicies(policies));
            policies.load();
        }
        binding.title.setText(signup ? "회원가입" : "로그인");
        binding.submit.setText(signup ? "가입하기" : "로그인");
        binding.switchMode.setText(signup ? "로그인으로 돌아가기" : "처음이에요! 회원가입");
        for (View view : new View[]{binding.confirmation, binding.nickname, binding.confirmationLabel, binding.nicknameLabel}) view.setVisibility(signup ? View.VISIBLE : View.GONE);
        binding.marketing.setVisibility(View.GONE);
        binding.switchMode.setOnClickListener(v -> { if (signup) finish(); else startActivity(new Intent(this, SignupActivity.class)); });
        binding.submit.setOnClickListener(v -> {
            java.util.List<com.example.footprint.data.model.Policy.Consent> consents = new java.util.ArrayList<>();
            if (signup) {
                if (!binding.terms.isChecked() || !binding.locationTerms.isChecked()) {
                    binding.error.setText("필수 약관에 모두 동의해 주세요.");
                    binding.error.setVisibility(View.VISIBLE);
                    return;
                }
                com.example.footprint.data.model.Policy termsPolicy = policies.find("TERMS");
                com.example.footprint.data.model.Policy locationPolicy = policies.find("LOCATION");
                if (termsPolicy == null || locationPolicy == null) {
                    binding.error.setText("약관을 불러오는 중이에요. 잠시 후 다시 눌러 주세요.");
                    binding.error.setVisibility(View.VISIBLE);
                    policies.load();
                    return;
                }
                consents.add(new com.example.footprint.data.model.Policy.Consent(termsPolicy, true));
                consents.add(new com.example.footprint.data.model.Policy.Consent(locationPolicy, true));
            }
            model.submit(signup, binding.email.getText().toString().trim(), binding.password.getText().toString(), binding.confirmation.getText().toString(), binding.nickname.getText().toString().trim(), binding.terms.isChecked(), consents);
        });
        model.state().observe(this, state -> {
            binding.submit.setEnabled(!state.loading);
            binding.switchMode.setEnabled(!state.loading);
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.error.setText(state.error == null ? "" : state.error);
            binding.error.setVisibility(state.error == null || state.error.isEmpty() ? View.GONE : View.VISIBLE);
            if (state.success) {
                model.consumeSuccess();
                if (signup) { Toast.makeText(this, "가입되었습니다. 로그인해 주세요.", Toast.LENGTH_LONG).show(); finish(); }
                else { startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
            }
        });
        if (com.example.footprint.data.repository.ProfileRepository.mock()) {
            binding.title.setText((signup ? "회원가입" : "로그인") + " · 개발용 Mock");
            if (!signup) {
                binding.error.setText("demo@example.com / footprint-demo-2026\nMock 계정·세션은 앱 프로세스 종료 시 초기화됩니다.");
                binding.error.setVisibility(View.VISIBLE);
            }
        }
    }

    /** 이용약관 · 개인정보 처리방침 · 위치기반서비스 약관 전문 */
    private void showPolicies(com.example.footprint.viewmodel.PolicyViewModel policies) {
        StringBuilder sb = new StringBuilder();
        String[][] items = {{"TERMS", "서비스 이용약관"}, {"PRIVACY", "개인정보 처리방침"}, {"LOCATION", "위치기반서비스 이용약관"}};
        for (String[] it : items) {
            com.example.footprint.data.model.Policy p = policies.find(it[0]);
            if (p == null) continue;
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("■ ").append(it[1]).append(" (").append(p.version).append(")\n").append(p.content);
        }
        if (sb.length() == 0) { policies.load(); Toast.makeText(this, "약관을 불러오는 중이에요. 잠시 후 다시 눌러 주세요.", Toast.LENGTH_SHORT).show(); return; }
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("약관 전문").setMessage(sb.toString()).setPositiveButton("확인", null).show();
    }
}
