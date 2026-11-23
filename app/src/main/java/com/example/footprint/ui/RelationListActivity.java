package com.example.footprint.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.example.footprint.databinding.ActivityRelationsBinding;
import com.example.footprint.viewmodel.RelationListViewModel;
import com.example.footprint.security.Session;
import java.util.*;

public final class RelationListActivity extends AppCompatActivity {
    private RelationListViewModel listModel;
    private RelationListViewModel sentModel;
    private String listType;
    private long ownerId;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Session.signedIn(this)) { Session.login(this); return; }
        ActivityRelationsBinding binding = ActivityRelationsBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        String type = getIntent().getStringExtra("type"); long id = getIntent().getLongExtra("userId",0);
        if (!java.util.Arrays.asList("followers","following","requests","blocks").contains(type)) { finish(); return; }
        boolean own = getIntent().getBooleanExtra("own", false);
        RelationListViewModel model = new ViewModelProvider(this).get(RelationListViewModel.class);
        listModel = model; listType = type; ownerId = id;
        binding.title.setText(type.equals("requests") ? "팔로우 요청" : type.equals("blocks") ? "차단 목록" : type.equals("followers") ? "팔로워" : "팔로잉");
        binding.back.setOnClickListener(v -> finish());
        binding.receivedHeading.setVisibility(type.equals("requests") ? View.VISIBLE : View.GONE);
        binding.sentSection.setVisibility(type.equals("requests") ? View.VISIBLE : View.GONE);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.retry.setOnClickListener(v -> model.load(type,id,false));
        binding.scroll.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (scroll,x,y,oldX,oldY) -> {
            if (y > oldY && !scroll.canScrollVertically(1) && Boolean.TRUE.equals(model.more.getValue())
                    && !Boolean.TRUE.equals(model.busy.getValue()) && model.error.getValue().isEmpty()) model.load(type,id,true);
        });
        if (type.equals("requests")) {
            sentModel = new ViewModelProvider(this).get("sent-requests", RelationListViewModel.class);
            binding.sentList.setLayoutManager(new LinearLayoutManager(this));
            sentModel.rows.observe(this, rows -> {
                binding.sentList.setAdapter(new RecyclerView.Adapter<Holder>() {
                    public Holder onCreateViewHolder(ViewGroup parent, int viewType) { return new Holder(new FriendCard(parent.getContext())); }
                    public void onBindViewHolder(Holder holder, int position) {
                        FriendCard card = (FriendCard) holder.itemView;
                        RelationListViewModel.Row row = rows.get(position);
                        card.name.setText(row.nickname); card.subtitle.setText("답을 기다리는 중이에요"); card.actions.removeAllViews();
                        TextView status = new TextView(RelationListActivity.this);
                        status.setText("대기 중"); status.setTextColor(android.graphics.Color.parseColor("#9A6B32"));
                        status.setTextSize(13); status.setPadding(card.dp(12),card.dp(8),card.dp(12),card.dp(8));
                        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
                        background.setColor(android.graphics.Color.parseColor("#FFE8C4")); background.setCornerRadius(card.dp(20));
                        status.setBackground(background); card.actions.addView(status);
                        card.setOnClickListener(v -> startActivity(new android.content.Intent(RelationListActivity.this,UserProfileActivity.class).putExtra("userId",row.userId)));
                    }
                    public int getItemCount() { return rows.size(); }
                });
                binding.sentMessage.setText(rows.isEmpty() ? "보낸 요청이 없습니다." : "");
                binding.sentMessage.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            });
            sentModel.error.observe(this, message -> {
                if (!message.isEmpty()) { binding.sentMessage.setText(message); binding.sentMessage.setVisibility(View.VISIBLE); }
            });
            sentModel.busy.observe(this, busy -> {
                if (busy) { binding.sentMessage.setText("불러오는 중…"); binding.sentMessage.setVisibility(View.VISIBLE); }
            });
        }
        model.rows.observe(this, rows -> {
            binding.list.setAdapter(new RecyclerView.Adapter<Holder>() {
                public Holder onCreateViewHolder(ViewGroup parent,int viewType) { return new Holder(new FriendCard(parent.getContext())); }
                public void onBindViewHolder(Holder holder,int position) {
                    FriendCard box = (FriendCard)holder.itemView; box.actions.removeAllViews(); RelationListViewModel.Row row = rows.get(position);
                    box.name.setText(row.nickname); box.subtitle.setText(type.equals("requests") ? "나를 팔로우하고 싶어 해요" : "프로필 보기"); View profile = box;
                    profile.setOnClickListener(v -> startActivity(new android.content.Intent(RelationListActivity.this, UserProfileActivity.class).putExtra("userId",row.userId)));
                    if (own && !type.equals("followers")) {
                        Button action = box.action(type.equals("requests") ? "수락" : type.equals("blocks") ? "차단 해제" : "언팔로우", true);
                        action.setOnClickListener(v -> new AlertDialog.Builder(RelationListActivity.this).setMessage(action.getText()+"할까요?").setNegativeButton("취소",null).setPositiveButton("확인",(d,w)->model.act(row,true)).show());
                        if (type.equals("requests")) { Button reject = box.action("거절", false); reject.setOnClickListener(v -> model.act(row,false)); }
                    }
                }
                public int getItemCount() { return rows.size(); }
            });
            binding.error.setText(rows.isEmpty() ? "목록이 비어 있습니다." : "");
            binding.error.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        });
        model.error.observe(this, message -> {
            binding.retry.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
            if (!message.isEmpty()) { binding.error.setText(message); binding.error.setVisibility(View.VISIBLE); }
        });
        model.busy.observe(this, busy -> {
            binding.retry.setEnabled(!busy);
            if (busy && model.rows.getValue().isEmpty()) { binding.error.setText("불러오는 중…"); binding.error.setVisibility(View.VISIBLE); }
        });
    }
    @Override protected void onResume() {
        super.onResume();
        if (listModel != null && Session.signedIn(this)) {
            listModel.load(listType,ownerId,false);
            if (sentModel != null) sentModel.load("sent",0,false);
        }
    }
    private static final class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
}
