package com.shiro.droid;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

final class Appearance {
    final boolean holo;
    final boolean dark;
    final int background, surface, ink, muted, accent;

    Appearance(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("shiro", 0);
        holo = prefs.getString("theme", "material").equals("holo");
        dark = prefs.getBoolean("dark", false);
        background = dark ? (holo ? 0xff000000 : 0xff1a1113) : (holo ? 0xffffffff : 0xfffff8f9);
        surface = dark ? (holo ? 0xff222222 : 0xff33262a) : (holo ? 0xfff3f3f3 : 0xfffeeaf0);
        ink = dark ? 0xfff0e0e3 : 0xff22191b;
        muted = dark ? 0xffc7b3ba : 0xff594a4f;
        accent = dark ? 0xffffb1c7 : 0xffbc0f52;
    }

    int dp(Context context, float value) { return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f); }
    GradientDrawable plate(Context context, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, holo ? 2 : 24));
        return drawable;
    }
    android.graphics.drawable.Drawable selection(Context context) {
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, plate(context, dark ? 0xff68505a : 0xffe7c4d0));
        states.addState(new int[]{android.R.attr.state_pressed}, plate(context, dark ? 0xff68505a : 0xffe7c4d0));
        states.addState(new int[]{}, plate(context, surface));
        return states;
    }
    android.graphics.drawable.Drawable rowSelection(Context context, boolean selected) {
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        int focus = dark ? 0xff806571 : 0xffd8a7b9;
        states.addState(new int[]{android.R.attr.state_focused}, plate(context, focus));
        states.addState(new int[]{android.R.attr.state_pressed}, plate(context, focus));
        states.addState(new int[]{}, selected ? plate(context, dark ? 0xff68505a : 0xffe7c4d0)
                : new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        return states;
    }
    int onAccent() { return dark ? 0xff501126 : Color.WHITE; }
}
