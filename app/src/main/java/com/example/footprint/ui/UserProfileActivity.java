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
    private boolean renderingProfile;
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
            View button = type.equals("followers") ? binding.followers : type.equals("following") ? binding.following : type.equals("requests") ? binding.requests : binding.blocks;
            button.setOnClickListener(v -> {
                if (model.profile.getValue() == null) return;
                startActivity(new android.content.Intent(this,RelationListActivity.class).putExtra("type",type).putExtra("userId",model.profile.getValue().userId.longValue()).putExtra("own", own));
            });
        }
        binding.settings.setVisibility(own ? View.VISIBLE : View.GONE);
        for (View view : new View[]{binding.requests,binding.blocks,binding.editActions,binding.logout,binding.bottomNav}) view.setVisibility(own ? View.VISIBLE : View.GONE);
        binding.editProfile.setOnClickListener(v -> binding.editSection.setVisibility(binding.editSection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        binding.logout.setOnClickListener(v -> com.example.footprint.security.Session.logout(this));
        binding.privateAccount.setOnCheckedChangeListener((button, checked) -> {
            if (renderingProfile || !own || model.profile.getValue() == null) return;
            model.save(model.profile.getValue().nickname, checked);
        });
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
        binding.title.setVisibility(ProfileRepository.mock() || !own ? View.VISIBLE : View.GONE);
        for (View view : new View[]{binding.nickname, binding.privateAccount, binding.chooseImage, binding.deleteImage, binding.save}) view.setVisibility(own ? View.VISIBLE : View.GONE);
        binding.copyCode.setOnClickListener(v -> {
            if (model.profile.getValue() == null || model.profile.getValue().friendCode == null) return;
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("친구 코드", model.profile.getValue().friendCode));
            android.widget.Toast.makeText(this,"친구 코드를 복사했습니다.",android.widget.Toast.LENGTH_SHORT).show();
        });
        binding.save.setOnClickListener(v -> model.save(binding.nickname.getText().toString(), binding.privateAccount.isChecked()));
        binding.retry.setOnClickListener(v -> model.load(id));
        binding.chooseImage.setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        binding.deleteImage.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("프로필 사진을 삭제할까요?")
                .setNegativeButton("취소", null).setPositiveButton("삭제", (d,w) -> model.deleteImage()).show());
        model.profile.observe(this, user -> {
            binding.follow.setText(user.blockedByMe ? "차단 해제" : "PENDING".equals(user.outgoingFollowStatus) ? "요청 중 · 취소" : "ACCEPTED".equals(user.outgoingFollowStatus) ? "팔로잉 · 취소" : "팔로우");
            binding.block.setText(user.blockedByMe ? "차단 해제" : "사용자 차단");
            renderingProfile = true;
            binding.nickname.setText(user.nickname); binding.privateAccount.setChecked(user.isPrivate);
            renderingProfile = false;
            binding.profileName.setText(user.nickname);
            binding.copyCode.setText("친구 코드 " + (user.friendCode == null ? "—" : user.friendCode));
            binding.guestbookCount.setText(user.guestbookCount + "\n방명록");
            binding.followers.setText(user.followerCount + "\n팔로워");
            binding.following.setText(user.followingCount + "\n팔로잉");
            binding.seeMap.setText(own ? "내가 쓴 방명록     ›" : "작성한 방명록     ›");
            binding.details.setVisibility(!own && user.isPrivate ? View.VISIBLE : View.GONE);
            binding.details.setText("비공개 계정");
            String image = user.profileImageUrl;
            if (image != null && image.startsWith("/")) image = okhttp3.HttpUrl.get(com.example.footprint.BuildConfig.API_BASE_URL).resolve(image).toString();
            Glide.with(this).load(image).placeholder(com.example.footprint.R.drawable.ic_basic_profile).error(com.example.footprint.R.drawable.ic_basic_profile).into(binding.photo);
        });
        binding.seeMap.setOnClickListener(v -> {
            com.example.footprint.data.model.UserProfile user = model.profile.getValue();
            if (user == null || user.userId == null) return;

            android.content.Intent intent = new android.content.Intent(this, ProfileMapActivity.class);
            intent.putExtra("TARGET_USER_ID", user.userId.longValue());
            intent.putExtra("TARGET_USER_NICKNAME", user.nickname);
            startActivity(intent);
        });
        binding.guestbookCount.setOnClickListener(v -> binding.seeMap.performClick());
        binding.navMyMap.setOnClickListener(v -> binding.seeMap.performClick());
        binding.navMap.setOnClickListener(v -> startActivity(new android.content.Intent(this,com.example.footprint.MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        binding.navFriends.setOnClickListener(v -> startActivity(new android.content.Intent(this,UserSearchActivity.class)));
        binding.navChallenges.setOnClickListener(v -> LocationConsent.request(this, () -> startActivity(new android.content.Intent(this,ChallengeActivity.class))));
        binding.navProfile.setSelected(true);
        binding.navProfile.setClickable(false);
        binding.navProfile.setTextColor(android.graphics.Color.parseColor("#2F7D46"));
        android.graphics.drawable.GradientDrawable selected = new android.graphics.drawable.GradientDrawable();
        selected.setColor(android.graphics.Color.parseColor("#DDF2CE")); selected.setCornerRadius(20 * getResources().getDisplayMetrics().density);
        binding.navProfile.setBackground(selected);
        model.error.observe(this, message -> {
            binding.error.setText(message);
            boolean failed = message != null && !message.isEmpty();
            binding.error.setVisibility(failed ? View.VISIBLE : View.GONE);
            binding.retry.setVisibility(failed ? View.VISIBLE : View.GONE);
            if (failed && model.profile.getValue() != null) {
                renderingProfile = true; binding.privateAccount.setChecked(model.profile.getValue().isPrivate); renderingProfile = false;
            }
        });
        model.busy.observe(this, busy -> { for (View view : new View[]{binding.save,binding.chooseImage,binding.deleteImage,binding.retry,binding.follow,binding.block,binding.privateAccount,binding.logout}) view.setEnabled(!busy); });
        if (model.profile.getValue() == null) model.load(id);
    }
    @Override protected void onResume() { super.onResume(); if (model != null) model.load(getIntent().getLongExtra("userId",0)); }
}
