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
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Session.signedIn(this)) { Session.login(this); return; }
        ActivityRelationsBinding binding = ActivityRelationsBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        String type = getIntent().getStringExtra("type"); long id = getIntent().getLongExtra("userId",0);
        RelationListViewModel model = new ViewModelProvider(this).get(RelationListViewModel.class);
        binding.title.setText(type.equals("requests") ? "받은 팔로우 요청" : type.equals("blocks") ? "차단 목록" : type.equals("followers") ? "팔로워" : "팔로잉");
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.retry.setOnClickListener(v -> model.load(type,id,false)); binding.more.setOnClickListener(v -> model.load(type,id,true));
        model.rows.observe(this, rows -> {
            binding.list.setAdapter(new RecyclerView.Adapter<Holder>() {
                public Holder onCreateViewHolder(ViewGroup parent,int viewType) { LinearLayout box = new LinearLayout(parent.getContext()); box.setOrientation(LinearLayout.VERTICAL); box.setLayoutParams(new RecyclerView.LayoutParams(-1,-2)); return new Holder(box); }
                public void onBindViewHolder(Holder holder,int position) {
                    LinearLayout box = (LinearLayout)holder.itemView; box.removeAllViews(); RelationListViewModel.Row row = rows.get(position);
                    Button profile = new Button(box.getContext()); profile.setText(row.nickname); box.addView(profile);
                    profile.setOnClickListener(v -> startActivity(new android.content.Intent(RelationListActivity.this, UserProfileActivity.class).putExtra("userId",row.userId)));
                    if (!type.equals("followers")) {
                        Button action = new Button(box.getContext()); action.setText(type.equals("requests") ? "승인" : type.equals("blocks") ? "차단 해제" : "언팔로우"); box.addView(action);
                        action.setOnClickListener(v -> new AlertDialog.Builder(RelationListActivity.this).setMessage(action.getText()+"할까요?").setNegativeButton("취소",null).setPositiveButton("확인",(d,w)->model.act(row,true)).show());
                        if (type.equals("requests")) { Button reject = new Button(box.getContext()); reject.setText("거절"); box.addView(reject); reject.setOnClickListener(v -> model.act(row,false)); }
                    }
                }
                public int getItemCount() { return rows.size(); }
            });
            binding.error.setText(rows.isEmpty() ? "목록이 비어 있습니다." : "");
        });
        model.error.observe(this, message -> { if (!message.isEmpty()) binding.error.setText(message); });
        model.busy.observe(this, busy -> { binding.retry.setEnabled(!busy); binding.more.setEnabled(!busy); });
        model.more.observe(this, more -> binding.more.setVisibility(more ? View.VISIBLE : View.GONE));
        if (state == null) model.load(type,id,false);
    }
    private static final class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
}
