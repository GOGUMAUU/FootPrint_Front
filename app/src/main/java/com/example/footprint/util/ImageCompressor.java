package com.example.footprint.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** 업로드 전에 사진을 줄여서 캐시 폴더에 JPEG로 저장합니다. */
public final class ImageCompressor {
    private static final int MAX_SIDE = 1280;
    private static final int QUALITY = 80;

    private ImageCompressor() {}

    public static File compress(Context ctx, Uri uri) throws IOException {
        ContentResolver cr = ctx.getContentResolver();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("사진을 읽을 수 없어요.");
        }

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = cr.openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new IOException("사진을 읽을 수 없어요.");

        float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bmp.getWidth(), bmp.getHeight()));
        int degrees = readRotation(cr, uri);
        Matrix m = new Matrix();
        if (scale < 1f) m.postScale(scale, scale);
        if (degrees != 0) m.postRotate(degrees);

        Bitmap out = bmp;
        if (!m.isIdentity()) {
            out = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (out != bmp) bmp.recycle();
        }

        File dir = new File(ctx.getCacheDir(), "upload");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "up_" + System.nanoTime() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            out.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos);
        }
        out.recycle();
        return file;
    }

    private static int readRotation(ContentResolver cr, Uri uri) {
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return 0;
            ExifInterface exif = new ExifInterface(in);
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}