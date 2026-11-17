package com.winlator.cmod.core;

import java.util.Locale;

public abstract class GPUInformation {

    private static String cachedRenderer;

    /** Renderer name, queried once and never null. */
    public static String rendererName() {
        if (cachedRenderer == null) {
            String renderer = null;
            try {
                renderer = getRenderer();
            }
            catch (Throwable e) {}
            cachedRenderer = renderer != null ? renderer.toLowerCase(Locale.ENGLISH) : "";
        }
        return cachedRenderer;
    }

    public static boolean isAdreno6xx() {
        return rendererName().matches(".*adreno[^6]+6[0-9]{2}.*");
    }

    public static boolean isAdreno7xx() {
        return rendererName().matches(".*adreno[^7]+7[0-9]{2}.*");
    }

    public static boolean isAdreno8xx() {
        return rendererName().matches(".*adreno[^8]+8[0-9]{2}.*");
    }

    public native static String getVersion();
    public native static String getRenderer();
    public native static long getMemorySize();
    public native static String[] enumerateExtensions();

    static {
        System.loadLibrary("winlator");
    }
}
