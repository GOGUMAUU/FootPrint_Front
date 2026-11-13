package com.example.footprint.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.example.footprint.databinding.ActivityProfileBinding;
import com.example.footprint.data.repository.ProfileRepository;
import com.example.footprint.viewmodel.UserViewModel;

public final class UserProfileActivity extends AppCompatActivity {
    private UserViewModel model;
    private final ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> { if (uri != null) model.upload(getApplication().getContentResolver(), uri); });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ActivityProfileBinding binding = ActivityProfileBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        model = new ViewModelProvider(this).get(UserViewModel.class);
        long id = getIntent().getLongExtra("userId", 0);
        boolean own = id == 0;
        for (String type : new String[]{"followers","following","requests","blocks"}) {
            if (!own && (type.equals("requests") || type.equals("blocks"))) continue;
            android.widget.Button button = new android.widget.Button(this);
            button.setText(type.equals("followers") ? "팔로워" : type.equals("following") ? "팔로잉" : type.equals("requests") ? "받은 요청" : "차단 목록");
            ((android.widget.LinearLayout) binding.settings.getParent()).addView(button);
            button.setOnClickListener(v -> {
                if (model.profile.getValue() == null) return;
                startActivity(new android.content.Intent(this,RelationListActivity.class).putExtra("type",type).putExtra("userId",model.profile.getValue().userId.longValue()));
            });
        }
        binding.settings.setVisibility(own ? View.VISIBLE : View.GONE);
        binding.settings.setOnClickListener(v -> startActivity(new android.content.Intent(this, AccountSettingsActivity.class)));
        binding.follow.setVisibility(own ? View.GONE : View.VISIBLE);
        binding.block.setVisibility(own ? View.GONE : View.VISIBLE);
        binding.follow.setOnClickListener(v -> {
            com.example.footprint.data.model.UserProfile user = model.profile.getValue(); if (user == null) return;
            String action = user.blockedByMe ? "unblock" : "NONE".equals(user.outgoingFollowStatus) ? "follow" : "unfollow";
            new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("관계를 변경할까요?").setNegativeButton("취소", null)
                    .setPositiveButton("확인", (d,w) -> model.relationship(action, id)).show();
        });
        binding.block.setOnClickListener(v -> {
            com.example.footprint.data.model.UserProfile user = model.profile.getValue(); if (user == null) return;
            new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(user.blockedByMe ? "차단을 해제할까요? 이전 관계는 복구되지 않습니다." : "차단하면 팔로우와 대기 요청이 정리됩니다.")
                    .setNegativeButton("취소", null).setPositiveButton("확인", (d,w) -> model.relationship(user.blockedByMe ? "unblock" : "block", id)).show();
        });
        binding.title.setText((own ? "마이페이지" : "사용자 프로필") + (ProfileRepository.mock() ? " · Mock" : ""));
        for (View view : new View[]{binding.nickname, binding.privateAccount, binding.chooseImage, binding.deleteImage, binding.save}) view.setVisibility(own ? View.VISIBLE : View.GONE);
        binding.copyCode.setOnClickListener(v -> {
            if (model.profile.getValue() == null || model.profile.getValue().friendCode == null) return;
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("친구 코드", model.profile.getValue().friendCode));
        });
        binding.save.setOnClickListener(v -> model.save(binding.nickname.getText().toString(), binding.privateAccount.isChecked()));
        binding.retry.setOnClickListener(v -> model.load(id));
        binding.chooseImage.setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        binding.deleteImage.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("프로필 사진을 삭제할까요?")
                .setNegativeButton("취소", null).setPositiveButton("삭제", (d,w) -> model.deleteImage()).show());
        model.profile.observe(this, user -> {
            binding.follow.setText(user.blockedByMe ? "차단 해제" : "PENDING".equals(user.outgoingFollowStatus) ? "요청 중 · 취소" : "ACCEPTED".equals(user.outgoingFollowStatus) ? "팔로잉 · 취소" : "팔로우");
            binding.block.setText(user.blockedByMe ? "차단 해제" : "사용자 차단");
            binding.nickname.setText(user.nickname); binding.privateAccount.setChecked(user.isPrivate);
            binding.details.setText(user.nickname + "\n" + (own && user.email != null ? user.email + "\n" : "")
                    + user.friendCode + "\n팔로워 " + user.followerCount + " · 팔로잉 " + user.followingCount + " · 방명록 " + user.guestbookCount
                    + (user.isPrivate ? "\n비공개 계정" : "\n공개 계정"));
            String image = user.profileImageUrl;
            if (image != null && image.startsWith("/")) image = okhttp3.HttpUrl.get(com.example.footprint.BuildConfig.API_BASE_URL).resolve(image).toString();
            Glide.with(this).load(image).placeholder(android.R.drawable.ic_menu_myplaces).error(android.R.drawable.ic_menu_myplaces).into(binding.photo);
        });
        model.error.observe(this, binding.error::setText);
        model.busy.observe(this, busy -> { for (View view : new View[]{binding.save,binding.chooseImage,binding.deleteImage,binding.retry,binding.follow,binding.block}) view.setEnabled(!busy); });
        if (model.profile.getValue() == null) model.load(id);
    }
    @Override protected void onResume() { super.onResume(); if (model != null) model.load(getIntent().getLongExtra("userId",0)); }
}
