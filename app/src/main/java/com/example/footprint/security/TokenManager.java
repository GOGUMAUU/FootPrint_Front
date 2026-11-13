package com.example.footprint.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.example.footprint.data.model.AuthTokens;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores the token pair atomically as a single authenticated ciphertext. */
public final class TokenManager {
    public static final androidx.lifecycle.MutableLiveData<Boolean> sessionInvalidated = new androidx.lifecycle.MutableLiveData<>(false);
    private static final String ALIAS = "footprint.auth.v1";
    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public TokenManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("auth_tokens", Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    public synchronized AuthTokens read() {
        String saved = preferences.getString("pair", null);
        if (saved == null) return null;
        try {
            String[] parts = saved.split(":");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            AuthTokens tokens = gson.fromJson(new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8), AuthTokens.class);
            if (tokens != null && tokens.isValid()) return tokens;
        } catch (Exception ignored) { /* Invalidated keys or corrupted storage require login. */ }
        clear();
        return null;
    }

    public synchronized void save(AuthTokens tokens) {
        if (tokens == null || !tokens.isValid()) throw new IllegalArgumentException("Missing tokens");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            String value = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                    + Base64.encodeToString(cipher.doFinal(gson.toJson(tokens).getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            if (!preferences.edit().putString("pair", value).commit()) throw new IllegalStateException("Token persistence failed");
            sessionInvalidated.postValue(false);
        } catch (Exception e) { throw new IllegalStateException("Cannot securely store credentials", e); }
    }

    public synchronized void clear() { preferences.edit().clear().commit(); sessionInvalidated.postValue(true); }
}
