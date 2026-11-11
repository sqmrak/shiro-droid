package com.shiro.droid;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

final class SystemBars {
    private SystemBars() {}

    static void apply(Activity activity, int top, int bottom) {
        if (Build.VERSION.SDK_INT >= 21) Api21.apply(activity.getWindow(), top, bottom);
    }
    private static boolean light(int color) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        return r * 299 + g * 587 + b * 114 >= 150000;
    }
    @android.annotation.TargetApi(21)
    private static final class Api21 {
        static void apply(Window window, int top, int bottom) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int flags = window.getDecorView().getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= 23) flags = Api23.statusFlags(flags, light(top));
            else if (light(top)) top = 0xff403438;
            if (Build.VERSION.SDK_INT >= 26) flags = Api26.navigationFlags(flags, light(bottom));
            else if (light(bottom)) bottom = 0xff403438;
            window.getDecorView().setSystemUiVisibility(flags);
            window.setStatusBarColor(top);
            window.setNavigationBarColor(bottom);
            if (Build.VERSION.SDK_INT >= 29) Api29.disableContrast(window);
        }
    }
    @android.annotation.TargetApi(23)
    private static final class Api23 {
        static int statusFlags(int flags, boolean darkIcons) {
            return darkIcons ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
    }
    @android.annotation.TargetApi(26)
    private static final class Api26 {
        static int navigationFlags(int flags, boolean darkIcons) {
            return darkIcons ? flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
    }
    @android.annotation.TargetApi(29)
    private static final class Api29 {
        static void disableContrast(Window window) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }
}
