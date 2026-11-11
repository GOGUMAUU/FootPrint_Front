package com.example.footprint;

import com.example.footprint.util.AuthValidation;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthValidationTest {
    @Test public void signupRequiresConsentAndMatchingPassword() {
        String password = "long passphrase for signup";
        assertNotNull(AuthValidation.validate("a@b.com", password, password, "name", false, true));
        assertNotNull(AuthValidation.validate("a@b.com", password, "different", "name", true, true));
        assertNull(AuthValidation.validate("a@b.com", password, password, "name", true, true));
    }
    @Test public void loginDoesNotApplyNewSignupPolicy() {
        assertNull(AuthValidation.validate("a@b.com", "old", null, null, false, false));
        assertNotNull(AuthValidation.validate("invalid", "old", null, null, false, false));
    }
    @Test public void passwordBoundariesAndUnicode() {
        assertNotNull(AuthValidation.validateNewPassword("a".repeat(14)));
        assertNull(AuthValidation.validateNewPassword("a".repeat(15)));
        assertNull(AuthValidation.validateNewPassword("a".repeat(64)));
        assertNotNull(AuthValidation.validateNewPassword("a".repeat(65)));
        assertNotNull(AuthValidation.validateNewPassword("😀".repeat(14)));
        assertNull(AuthValidation.validateNewPassword("😀".repeat(15)));
    }
}
