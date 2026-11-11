package com.example.footprint.util;

public final class ProfileImagePolicy {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private ProfileImagePolicy() {}
    public static boolean accepts(String mimeType, long size) {
        return size > 0 && size <= MAX_BYTES && ("image/jpeg".equals(mimeType)
                || "image/png".equals(mimeType) || "image/webp".equals(mimeType));
    }
}
