package com.example.footprint.util;

public final class AuthValidation {
    private AuthValidation() {}
    public static String validate(String email, String password, String confirmation,
                                  String nickname, boolean terms, boolean signup) {
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) return "이메일 형식을 확인해 주세요.";
        if (password == null || password.isEmpty()) return "비밀번호를 입력해 주세요.";
        if (!signup) return null;
        String passwordError = validateNewPassword(password);
        if (passwordError != null) return passwordError;
        if (!password.equals(confirmation)) return "비밀번호 확인이 일치하지 않습니다.";
        if (nickname == null || nickname.trim().isEmpty()) return "닉네임을 입력해 주세요.";
        if (!terms) return "필수 약관 동의가 필요합니다.";
        return null;
    }
    public static String validateNewPassword(String password) {
        int length = password == null ? 0 : password.codePointCount(0, password.length());
        return length < 15 || length > 64 ? "비밀번호는 15자 이상 64자 이하로 입력해 주세요." : null;
    }
}
