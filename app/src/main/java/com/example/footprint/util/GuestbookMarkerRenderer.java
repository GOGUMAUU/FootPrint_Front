package com.example.footprint.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.core.content.res.ResourcesCompat;

import com.example.footprint.R;

public final class GuestbookMarkerRenderer {
    private GuestbookMarkerRenderer() {}

    // 주황, 노랑, 초록, 파랑, 보라
    private static final int[] BORDER_COLORS = {
            0xFFF5A04A, 0xFFF2C94C, 0xFF6FBF73, 0xFF5AB4E8, 0xFFA98BE0
    };

    // 편지 말풍선: 연핑크
    public static final int LETTER_FILL = 0xFFFFF0F3;
    public static final int LETTER_BORDER = 0xFFF4A6B8;
    public static final int LETTER_TEXT = 0xFF8A4B5C;

    public static Bitmap createMarkerBitmap(Context context, String shortText, boolean representative) {
        String text = (shortText == null || shortText.trim().isEmpty()) ? "방명록" : shortText.trim();
        // 같은 글은 항상 같은 색 (지도를 움직여도 색이 안 바뀜)
        int color = BORDER_COLORS[(text.hashCode() & 0x7fffffff) % BORDER_COLORS.length];
        return render(context, text, 0xFFFFFDF4, color, 0xFF5E4128);
    }

    /**
     * 그 자리에서 열리는 편지 마커: 연핑크 동그라미 안에 💌 (말풍선 대신).
     * 다른 fp 버튼처럼 아래쪽에 살짝 두께감. 중심이 실제 좌표 → anchor (0.5, 0.5)
     */
    public static Bitmap createLetterBitmap(Context context, String ignored) {
        float d = context.getResources().getDisplayMetrics().density;
        float size = 44 * d;          // 동그라미 지름
        float depth = 3 * d;          // 아래 두께감
        float stroke = 3 * d;
        float margin = 3 * d;         // 누르기 쉽게 투명 여백
        int w = (int) Math.ceil(size + 2 * margin);
        int h = (int) Math.ceil(size + depth + 2 * margin);

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float cx = w / 2f;
        float cy = margin + size / 2f;
        float r = size / 2f - stroke / 2f;

        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0xFFF4C3CF);
        canvas.drawCircle(cx, cy + depth, r + stroke / 2f, shadow);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(LETTER_FILL);
        canvas.drawCircle(cx, cy, r, fill);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(stroke);
        line.setColor(LETTER_BORDER);
        canvas.drawCircle(cx, cy, r, line);

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextSize(22 * d);
        tp.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = tp.getFontMetrics();
        canvas.drawText("💌", cx, cy - (fm.ascent + fm.descent) / 2f, tp);
        return bitmap;
    }

    private static Bitmap render(Context context, String text, int fillColor, int color, int textColor) {
        float d = context.getResources().getDisplayMetrics().density;
        float maxW = context.getResources().getDisplayMetrics().widthPixels / 3f; // 폰 가로의 1/3

        float stroke = 3 * d;
        float padH = 12 * d;
        float padV = 8 * d;
        float tailH = 9 * d;
        float tailW = 16 * d;
        float margin = 4 * d; // 좌우·위 투명 여백 (누르기 쉽게)

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(textColor);
        tp.setTextSize(14 * d);
        try {
            Typeface tf = ResourcesCompat.getFont(context, R.font.jua);
            if (tf != null) tp.setTypeface(tf);
        } catch (Exception ignored) {
        }

        float maxTextW = maxW - 2 * padH - 2 * stroke - 2 * margin;
        CharSequence shown = TextUtils.ellipsize(text, tp, maxTextW, TextUtils.TruncateAt.END);
        float textW = tp.measureText(shown, 0, shown.length());
        Paint.FontMetrics fm = tp.getFontMetrics();
        float textH = fm.descent - fm.ascent;

        float bubbleW = textW + 2 * padH + stroke;
        float bubbleH = textH + 2 * padV + stroke;
        int w = (int) Math.ceil(bubbleW + 2 * margin);
        int h = (int) Math.ceil(margin + bubbleH + tailH);

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(fillColor);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(stroke);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(color);

        float left = margin + stroke / 2f;
        float top = margin + stroke / 2f;
        float right = w - margin - stroke / 2f;
        float bottom = margin + bubbleH - stroke / 2f;
        RectF rect = new RectF(left, top, right, bottom);
        float r = 16 * d;
        canvas.drawRoundRect(rect, r, r, fill);
        canvas.drawRoundRect(rect, r, r, line);

        // 꼬리: 아래 가운데 끝이 실제 좌표를 가리킴
        float cx = w / 2f;
        float baseY = bottom - stroke / 2f - 0.5f;
        Path tail = new Path();
        tail.moveTo(cx - tailW / 2f, baseY);
        tail.lineTo(cx, h - stroke / 2f);
        tail.lineTo(cx + tailW / 2f, baseY);
        tail.close();
        canvas.drawPath(tail, fill);
        Path sides = new Path();
        sides.moveTo(cx - tailW / 2f, baseY);
        sides.lineTo(cx, h - stroke / 2f);
        sides.lineTo(cx + tailW / 2f, baseY);
        canvas.drawPath(sides, line);

        canvas.drawText(shown, 0, shown.length(),
                left + stroke / 2f + padH,
                (top + bottom) / 2f - (fm.ascent + fm.descent) / 2f,
                tp);
        return bitmap;
    }
}