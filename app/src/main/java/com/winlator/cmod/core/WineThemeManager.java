package com.winlator.cmod.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;

import com.winlator.cmod.R;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xserver.ScreenInfo;

import java.io.File;

public abstract class WineThemeManager {
    public enum Theme {LIGHT, DARK}
    public enum BackgroundType {IMAGE, COLOR}
    public static final String DEFAULT_DESKTOP_THEME = Theme.LIGHT+","+BackgroundType.IMAGE+",#0B1220";


    /** Windows 11 light: white title bars, light grey chrome, #0078D4 accent. */
    private static final String[][] LIGHT_COLORS = {
        {"ActiveBorder", "243 243 243"}, {"ActiveTitle", "255 255 255"},
        {"ButtonAlternateFace", "251 251 251"}, {"ButtonDkShadow", "160 160 160"},
        {"ButtonFace", "243 243 243"}, {"ButtonHilight", "255 255 255"},
        {"ButtonLight", "250 250 250"}, {"ButtonShadow", "205 205 205"},
        {"ButtonText", "26 26 26"}, {"GradientActiveTitle", "255 255 255"},
        {"GradientInactiveTitle", "243 243 243"}, {"GrayText", "150 150 150"},
        {"Hilight", "0 120 212"}, {"HilightText", "255 255 255"},
        {"HotTrackingColor", "0 120 212"}, {"InactiveBorder", "243 243 243"},
        {"InactiveTitle", "243 243 243"}, {"InactiveTitleText", "130 130 130"},
        {"InfoText", "26 26 26"}, {"InfoWindow", "255 255 255"},
        {"Menu", "249 249 249"}, {"MenuBar", "249 249 249"},
        {"MenuHilight", "0 120 212"}, {"MenuText", "26 26 26"},
        {"Scrollbar", "243 243 243"}, {"TitleText", "26 26 26"},
        {"Window", "255 255 255"}, {"WindowFrame", "218 218 218"},
        {"WindowText", "26 26 26"}
    };

    /** Windows 11 dark. */
    private static final String[][] DARK_COLORS = {
        {"ActiveBorder", "43 43 43"}, {"ActiveTitle", "32 32 32"},
        {"ButtonAlternateFace", "43 43 43"}, {"ButtonDkShadow", "20 20 20"},
        {"ButtonFace", "43 43 43"}, {"ButtonHilight", "60 60 60"},
        {"ButtonLight", "55 55 55"}, {"ButtonShadow", "25 25 25"},
        {"ButtonText", "255 255 255"}, {"GradientActiveTitle", "32 32 32"},
        {"GradientInactiveTitle", "43 43 43"}, {"GrayText", "140 140 140"},
        {"Hilight", "0 120 212"}, {"HilightText", "255 255 255"},
        {"HotTrackingColor", "76 194 255"}, {"InactiveBorder", "43 43 43"},
        {"InactiveTitle", "43 43 43"}, {"InactiveTitleText", "150 150 150"},
        {"InfoText", "255 255 255"}, {"InfoWindow", "43 43 43"},
        {"Menu", "43 43 43"}, {"MenuBar", "32 32 32"},
        {"MenuHilight", "0 120 212"}, {"MenuText", "255 255 255"},
        {"Scrollbar", "43 43 43"}, {"TitleText", "255 255 255"},
        {"Window", "32 32 32"}, {"WindowFrame", "70 70 70"},
        {"WindowText", "255 255 255"}
    };

    public static class ThemeInfo {
        public final Theme theme;
        public final BackgroundType backgroundType;
        public final int backgroundColor;

        public ThemeInfo(String value) {
            String[] values = value.split(",");
            theme = Theme.valueOf(values[0]);
            if (values.length < 3) {
                backgroundColor = Color.parseColor(values[1]);
                backgroundType = BackgroundType.IMAGE;
            }
            else {
                backgroundType = BackgroundType.valueOf(values[1]);
                backgroundColor = Color.parseColor(values[2]);
            }
        }
    }

    public static void apply(Context context, ThemeInfo themeInfo, ScreenInfo screenInfo) {
        File rootDir = ImageFs.find(context).getRootDir();
        File userRegFile = new File(rootDir, ImageFs.WINEPREFIX+"/user.reg");
        String background = Color.red(themeInfo.backgroundColor)+" "+Color.green(themeInfo.backgroundColor)+" "+Color.blue(themeInfo.backgroundColor);

        if (themeInfo.backgroundType == BackgroundType.IMAGE) createWallpaperBMPFile(context, screenInfo);

        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            if (themeInfo.backgroundType == BackgroundType.IMAGE) {
                registryEditor.setStringValue("Control Panel\\Desktop", "Wallpaper", ImageFs.CACHE_PATH+"/wallpaper.bmp");
            }
            else registryEditor.removeValue("Control Panel\\Desktop", "Wallpaper");

            // WinArt: flat Windows 10/11 style palette
            String[][] palette = themeInfo.theme == Theme.LIGHT ? LIGHT_COLORS : DARK_COLORS;
            for (String[] entry : palette) registryEditor.setStringValue("Control Panel\\Colors", entry[0], entry[1]);
            registryEditor.setStringValue("Control Panel\\Colors", "Background", background);
            // crisper text, like a modern Windows desktop
            registryEditor.setStringValue("Control Panel\\Desktop", "FontSmoothing", "2");
            registryEditor.setDwordValue("Control Panel\\Desktop", "FontSmoothingType", 2);
            registryEditor.setDwordValue("Control Panel\\Desktop", "FontSmoothingGamma", 1400);
            registryEditor.setDwordValue("Control Panel\\Desktop", "FontSmoothingOrientation", 1);
        }
    }

    private static void createWallpaperBMPFile(Context context, ScreenInfo screenInfo) {
        final int outputHeight = 480;
        int outputWidth = (int)Math.ceil(((float)outputHeight / screenInfo.height) * screenInfo.width);

        Bitmap outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Canvas canvas = new Canvas(outputBitmap);

        File userWallpaperFile = getUserWallpaperFile(context);
        if (userWallpaperFile.isFile()) {
            Bitmap image = BitmapFactory.decodeFile(userWallpaperFile.getPath());
            Rect srcRect = new Rect(0, 0, image.getWidth(), image.getHeight());
            Rect dstRect = new Rect(0, 0, outputWidth, outputHeight);
            canvas.drawBitmap(image, srcRect, dstRect, paint);
        }
        else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inTargetDensity = DisplayMetrics.DENSITY_HIGH;
            Bitmap wallpaperBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wallpaper, options);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff01579b);
            canvas.drawRect(0, 0, outputWidth, outputHeight * 0.5f, paint);
            paint.setColor(0xff0277bd);
            canvas.drawRect(0, outputHeight * 0.5f, outputWidth, outputHeight, paint);

            float targetSize = outputHeight * (320.0f / 480.0f);
            float centerX = (outputWidth - targetSize) * 0.5f;
            float centerY = (outputHeight - targetSize) * 0.5f;
            Rect srcRect = new Rect(0, 0, wallpaperBitmap.getWidth(), wallpaperBitmap.getHeight());
            RectF dstRect = new RectF(centerX, centerY, centerX + targetSize, centerY + targetSize);
            canvas.drawBitmap(wallpaperBitmap, srcRect, dstRect, paint);
        }

        ImageFs imageFs = ImageFs.find(context);
        MSBitmap.create(outputBitmap, new File(imageFs.getRootDir(), ImageFs.CACHE_PATH+"/wallpaper.bmp"));
    }

    public static File getUserWallpaperFile(Context context) {
        return new File(ImageFs.find(context).getRootDir(), ImageFs.CONFIG_PATH+"/user-wallpaper.png");
    }
}
