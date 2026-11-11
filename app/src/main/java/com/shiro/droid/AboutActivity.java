package com.shiro.droid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class AboutActivity extends Activity {
    private Appearance theme;
    private LinearLayout page;
    private int animatedSections;
    private long createdAt;

    @Override public void onCreate(Bundle state) {
        setTheme(getSharedPreferences("shiro", 0).getBoolean("dark", false)
                ? R.style.ShiroDark : R.style.ShiroLight);
        super.onCreate(state);
        createdAt = android.os.SystemClock.uptimeMillis();
        theme = new Appearance(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.background);
        ContentColumn column = new ContentColumn(this);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        page.setPadding(pad, pad, pad, pad);
        column.addView(page);
        scroll.addView(column);
        hero();
        section(R.string.about_device);
        row(R.string.about_model, deviceName(), 0, null);
        row(R.string.about_android, Build.VERSION.RELEASE, 0, null);
        row(R.string.about_architecture, Build.CPU_ABI, 0, null);
        row(R.string.about_tls, getString(R.string.about_tls_value), 0, null);
        section(R.string.about_shiro);
        row(R.string.about_how, getString(R.string.about_how_value), 0, null);
        row(R.string.about_protocol, "WireGuard · Cloudflare WARP", 0, null);
        row(R.string.about_compatibility, getString(R.string.about_compatibility_value), 0, null);
        row(R.string.about_security, getString(R.string.about_security_value), 0, null);
        section(R.string.about_credits);
        row(R.string.about_developer, "sqmrak", R.drawable.author_sqmrak, null);
        row(R.string.about_codeveloper, "@QWERTYU1OPZ", R.drawable.author_qwerty, null);
        row(R.string.about_license, "GNU GPL v3", 0, null);
        section(R.string.about_links);
        row(R.string.about_telegram, "t.me/sqmrakdev", 0, new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sqmrakdev"))); }
                catch (ActivityNotFoundException e) {
                    ShiroDialog dialog = new ShiroDialog(AboutActivity.this, getString(R.string.about_links), R.drawable.sticker_error);
                    dialog.message(getString(R.string.about_no_browser));
                    final ShiroDialog current = dialog;
                    dialog.button(getString(android.R.string.ok), new View.OnClickListener() { public void onClick(View view) { current.dismiss(); } });
                    dialog.show();
                }
            }
        });
        setContentView(ScreenChrome.wrap(this, theme, getString(R.string.about), scroll));
    }
    private int dp(float value) { return theme.dp(this, value); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(Typeface.createFromAsset(getAssets(), bold ? "fonts/Rubik-Bold.ttf" : "fonts/Rubik-Regular.ttf"));
        return view;
    }
    private String version() {
        try {
            String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return name == null ? getString(R.string.about_unknown) : name;
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.about_unknown);
        }
    }
    private String deviceName() {
        String name = null;
        if (Build.VERSION.SDK_INT >= 17) {
            try { name = android.provider.Settings.Global.getString(getContentResolver(), "device_name"); }
            catch (RuntimeException e) { android.util.Log.w("shiro", "cannot read device name", e); }
        }
        if (name != null && !name.trim().isEmpty()) return name.trim();
        String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        if (model.toLowerCase(java.util.Locale.US).startsWith(maker.toLowerCase(java.util.Locale.US))) return model;
        return maker.isEmpty() ? model : Character.toUpperCase(maker.charAt(0)) + maker.substring(1) + " " + model;
    }
    private void hero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(0, dp(16), 0, dp(8));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        hero.addView(icon, new LinearLayout.LayoutParams(dp(88), dp(88)));
        TextView name = text("Shiro", 30, theme.ink, true);
        name.setPadding(0, dp(8), 0, 0);
        hero.addView(name);
        hero.addView(text(getString(R.string.about_tagline), 14, theme.muted, false));
        LinearLayout tiles = new LinearLayout(this);
        tiles.setPadding(0, dp(14), 0, 0);
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1);
        first.rightMargin = dp(5);
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, -2, 1);
        second.leftMargin = dp(5);
        tiles.addView(tile(getString(R.string.about_version), version()), first);
        tiles.addView(tile("Android", Build.VERSION.RELEASE + " · " + Build.CPU_ABI), second);
        hero.addView(tiles, new LinearLayout.LayoutParams(-1, -2));
        page.addView(hero);
    }
    private View tile(String title, String value) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(10), dp(10), dp(10), dp(10));
        tile.setBackgroundDrawable(theme.plate(this, theme.surface));
        tile.addView(text(title, 12, theme.muted, false));
        TextView label = text(value, 17, theme.ink, true);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(label);
        return tile;
    }
    private void section(int title) {
        TextView heading = text(getString(title), 14, theme.accent, true);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(dp(8), dp(16), dp(8), dp(6));
        page.addView(heading);
        animateSection(heading);
    }
    private void animateSection(View view) {
        if (!Tween.enabled() || android.os.SystemClock.uptimeMillis() - createdAt > 400 || animatedSections >= 8) return;
        long delay = animatedSections++ * (theme.holo ? 55L : 35L);
        if (theme.holo) ViewFx.enter(view, theme.dp(this, 20), 1f, delay, Motion.ENTER_MS, Motion.ENTER);
        else ViewFx.enter(view, 0f, .9f, delay, Motion.SPRING_MS, Motion.SPRING);
    }
    private void row(int title, String value, int image, View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackgroundDrawable(action == null ? theme.plate(this, theme.surface) : theme.selection(this));
        if (image != 0) {
            ImageView avatar = new AvatarView(this);
            avatar.setImageResource(image);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));
        }
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        if (image != 0) labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(getString(title), 16, theme.ink, false));
        labels.addView(text(value == null ? getString(R.string.about_unknown) : value, 13, theme.muted, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        if (action != null) {
            row.addView(text("›", 24, theme.muted, false));
            row.setFocusable(true);
            row.setOnClickListener(action);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(3);
        page.addView(row, params);
    }
}
