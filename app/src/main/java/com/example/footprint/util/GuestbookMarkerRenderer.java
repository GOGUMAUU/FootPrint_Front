package com.example.footprint.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;

public final class GuestbookMarkerRenderer {
    private GuestbookMarkerRenderer() {}

    // Simple speech-bubble marker prototype. Replace inner block with photo thumbnail rendering (Glide -> Bitmap) later.
    public static void apply(Context context, Marker marker, String shortText, boolean representative) {
        float density = context.getResources().getDisplayMetrics().density;
        int w = (int) ((representative ? 150 : 190) * density);
        int h = (int) ((representative ? 64 : 82) * density);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        canvas.drawRoundRect(new RectF(2*density, 2*density, w-2*density, h-14*density), 18*density, 18*density, p);
        p.setColor(0xFF2457E6);
        canvas.drawCircle(22*density, 22*density, 12*density, p);
        p.setColor(0xFF111111);
        p.setTextSize((representative ? 12 : 13) * density);
        String text = shortText == null ? "방명록" : shortText;
        if (text.length() > 15) text = text.substring(0, 15) + "…";
        canvas.drawText(text, 42*density, 28*density, p);
        p.setColor(0xFFFFFFFF);
        android.graphics.Path tail = new android.graphics.Path();
        tail.moveTo(w/2f - 10*density, h-16*density);
        tail.lineTo(w/2f + 10*density, h-16*density);
        tail.lineTo(w/2f, h);
        tail.close();
        canvas.drawPath(tail, p);
        marker.setIcon(OverlayImage.fromBitmap(bitmap));
        marker.setWidth(w);
        marker.setHeight(h);
        marker.setAnchor(new android.graphics.PointF(0.5f, 1f));
        marker.setHideCollidedMarkers(true);
        marker.setHideCollidedSymbols(true);
    }
}
