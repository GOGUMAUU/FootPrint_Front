package com.example.footprint.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import com.example.footprint.R;
import de.hdodenhof.circleimageview.CircleImageView;

/** Shared rounded row for search results and relationship lists. */
final class FriendCard extends LinearLayout {
    final CircleImageView avatar;
    final TextView name, subtitle;
    final LinearLayout actions;
    FriendCard(Context context) {
        super(context); setOrientation(VERTICAL);
        setLayoutParams(new androidx.recyclerview.widget.RecyclerView.LayoutParams(-1,-2));
        android.view.LayoutInflater.from(context).inflate(R.layout.item_friend_card,this,true);
        avatar=findViewById(R.id.friendAvatar); name=findViewById(R.id.friendName);
        subtitle=findViewById(R.id.friendSubtitle); actions=findViewById(R.id.friendActions);
    }
    Button action(String label, boolean primary) {
        com.google.android.material.button.MaterialButton b=new com.google.android.material.button.MaterialButton(getContext());
        b.setText(label); b.setAllCaps(false); b.setTextSize(13); b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0); b.setCornerRadius(dp(22));
        b.setPadding(dp(12),0,dp(12),0); b.setInsetTop(dp(6)); b.setInsetBottom(dp(6));
        LayoutParams params=new LayoutParams(-2,dp(48)); params.setMarginStart(actions.getChildCount() == 0 ? 0 : dp(8)); b.setLayoutParams(params);
        b.setTextColor(Color.parseColor(primary ? "#FFFFFF":"#76583D")); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(primary ? "#2F7D46":"#FFF6DE")));
        b.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#EAD9A8"))); b.setStrokeWidth(primary?0:dp(2)); actions.addView(b); return b;
    }
    int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
