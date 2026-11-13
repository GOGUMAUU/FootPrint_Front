package com.example.footprint.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.example.footprint.viewmodel.PolicyViewModel;
import com.example.footprint.data.model.Policy;
import com.example.footprint.data.repository.ProfileRepository;
import com.example.footprint.security.Session;
import com.example.footprint.api.*;
import com.google.gson.*;
import retrofit2.*;

/** Session-scoped consent cache; every fresh session rechecks the published policy. */
public final class LocationConsent {
    private static final java.util.Set<AppCompatActivity> active = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public static void request(AppCompatActivity activity, Runnable allowed) {
        if (!Session.signedIn(activity)) return;
        if (Session.locationConsent) { allowed.run(); return; }
        if (!active.add(activity)) return;
        long generation = Session.generation();
        PolicyViewModel model = new ViewModelProvider(activity).get(PolicyViewModel.class);
        // This helper owns the policy observers on location screens. Remove failed
        // attempts before registering again so a later success opens only one dialog.
        model.policies.removeObservers(activity);
        model.error.removeObservers(activity);
        model.error.setValue("");
        final boolean[] shown = {false};
        final boolean[] submitting = {false};
        model.policies.observe(activity, new androidx.lifecycle.Observer<java.util.List<Policy>>() {
          public void onChanged(java.util.List<Policy> values) {
            Policy policy = model.find("LOCATION");
            if (policy == null || shown[0]) return;
            shown[0] = true;
            model.policies.removeObserver(this);
            new AlertDialog.Builder(activity).setTitle("위치기반서비스 · " + policy.version).setMessage(policy.content)
                    .setNegativeButton("나중에", null).setOnDismissListener(dialog -> { if (!submitting[0]) active.remove(activity); }).setPositiveButton("동의", (d,w) -> {
                        if (generation != Session.generation() || !Session.signedIn(activity)) return;
                        if (ProfileRepository.mock()) { Session.locationConsent = true; allowed.run(); return; }
                        submitting[0] = true;
                        JsonObject body = new JsonObject();
                        body.add("consents", new Gson().toJsonTree(java.util.Collections.singletonList(new Policy.Consent(policy, true))));
                        ApiClient.create(PolicyApi.class).consent(body).enqueue(new Callback<Void>() {
                            public void onResponse(Call<Void> c, Response<Void> r) {
                                active.remove(activity);
                                if (activity.isDestroyed() || activity.isFinishing()) return;
                                if (r.isSuccessful() && generation == Session.generation() && Session.signedIn(activity)) { Session.locationConsent = true; if (!activity.isDestroyed()) allowed.run(); }
                                else android.widget.Toast.makeText(activity, "위치 동의를 저장하지 못했습니다.", android.widget.Toast.LENGTH_LONG).show();
                            }
                            public void onFailure(Call<Void> c, Throwable t) { active.remove(activity); if (!activity.isDestroyed()) android.widget.Toast.makeText(activity, "위치 동의 실패. 다시 시도해 주세요.", android.widget.Toast.LENGTH_LONG).show(); }
                        });
                    }).show();
          }
        });
        model.error.observe(activity, message -> { if (!message.isEmpty()) { active.remove(activity); android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_LONG).show(); } });
        model.load();
    }
}
