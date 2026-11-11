package com.shiro.droid;

import android.app.Activity;


import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public final class SettingsActivity extends Activity {
    private Appearance theme;
    private SharedPreferences prefs;
    private LinearLayout page, group;
    private ScrollView scroll;
    private int animatedSections;
    private long createdAt;
    @Override public void onCreate(Bundle state) {
        setTheme(getSharedPreferences("shiro", 0).getBoolean("dark", false)
                ? R.style.ShiroDark : R.style.ShiroLight);
        super.onCreate(state);
        createdAt = android.os.SystemClock.uptimeMillis();
        prefs = getSharedPreferences("shiro", 0);
        build();
    }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/Rubik-Regular.ttf"));
        return view;
    }
    private void build() {
        theme = new Appearance(this);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(theme.background);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = theme.dp(this, 12);
        page.setPadding(pad, theme.dp(this, 6), pad, theme.dp(this, 24));
        ContentColumn column = new ContentColumn(this);
        column.addView(page);
        scroll.addView(column);
        section(getString(R.string.main));
        toggle(getString(R.string.startup), R.drawable.material_glyph_rocket, "connect_at_startup", false);
        toggle(getString(R.string.reconnect), R.drawable.material_glyph_reconnect, "auto_reconnect", true);
        toggle(getString(R.string.failover), R.drawable.material_glyph_shuffle, "failover", true);
        section(getString(R.string.appearance));
        row(getString(R.string.theme), R.drawable.material_glyph_palette, theme.holo ? "Holo" : "Material You", new View.OnClickListener() {
            @Override public void onClick(View v) {
                final ShiroDialog dialog = new ShiroDialog(SettingsActivity.this, getString(R.string.theme), R.drawable.sticker_ask);
                dialog.choice("Material You", !theme.holo, new Runnable() { public void run() {
                    changeTheme(v, "material", null);
                }});
                dialog.choice("Holo", theme.holo, new Runnable() { public void run() {
                    changeTheme(v, "holo", null);
                }});
                dialog.cancelButton();
                dialog.show();
            }
        });
        toggle(getString(R.string.dark), R.drawable.material_glyph_palette, "dark", false);
        section(getString(R.string.advanced));
        edit(getString(R.string.dns), R.drawable.material_glyph_shield, "dns", "1.1.1.1", false);
        edit(getString(R.string.direct), R.drawable.material_glyph_split, "direct", "", true);
        edit(getString(R.string.blocked), R.drawable.material_glyph_shield, "block", "", true);
        edit(getString(R.string.proxy), R.drawable.material_glyph_globe, "registration_proxy", "", false);
        TextView hint = label(getString(R.string.hint), 13, theme.muted);
        hint.setPadding(0, theme.dp(this, 12), 0, 0);
        page.addView(hint);
        section(getString(R.string.about_shiro));
        row(getString(R.string.about), R.drawable.material_glyph_info, "", new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(SettingsActivity.this, AboutActivity.class)); }
        });
        setContentView(ScreenChrome.wrap(this, theme, getString(R.string.settings), scroll));
    }
    private void section(String title) {
        TextView heading = label(title, 14, theme.accent);
        heading.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/Rubik-Bold.ttf"));
        heading.setGravity(android.view.Gravity.CENTER);
        heading.setPadding(theme.dp(this, 8), theme.dp(this, 16), theme.dp(this, 8), theme.dp(this, 6));
        page.addView(heading);
        group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackgroundDrawable(theme.plate(this, theme.surface));
        page.addView(group, new LinearLayout.LayoutParams(-1, -2));
        animateSection(heading);
        animateSection(group);
    }
    private void animateSection(View view) {
        if (!Tween.enabled() || android.os.SystemClock.uptimeMillis() - createdAt > 400 || animatedSections >= 8) return;
        long delay = animatedSections++ * (theme.holo ? 55L : 35L);
        if (theme.holo) ViewFx.enter(view, theme.dp(this, 20), 1f, delay, Motion.ENTER_MS, Motion.ENTER);
        else ViewFx.enter(view, 0f, .9f, delay, Motion.SPRING_MS, Motion.SPRING);
    }
    private LinearLayout line(int icon) {
        if (group.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(theme.dark ? 0x33ffffff : 0x2222191b);
            LinearLayout.LayoutParams rule = new LinearLayout.LayoutParams(-1, Math.max(1, theme.dp(this, 0.5f)));
            rule.leftMargin = theme.dp(this, 14);
            group.addView(divider, rule);
        }
        LinearLayout line = new LinearLayout(this);
        line.setGravity(android.view.Gravity.CENTER_VERTICAL);
        line.setMinimumHeight(theme.dp(this, 54));
        line.setPadding(theme.dp(this, 14), 0, theme.dp(this, 14), 0);
        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setImageResource(icon);
        image.setColorFilter(theme.ink);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(theme.dp(this, 22), theme.dp(this, 22));
        imageParams.rightMargin = theme.dp(this, 12);
        line.addView(image, imageParams);
        group.addView(line, new LinearLayout.LayoutParams(-1, -2));
        return line;
    }
    private void row(String title, int icon, String value, View.OnClickListener click) {
        LinearLayout line = line(icon);
        TextView name = label(title, 16, theme.ink);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        line.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView detail = label(value, 14, theme.muted);
        detail.setSingleLine(true);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        detail.setGravity(android.view.Gravity.RIGHT);
        detail.setMaxWidth(theme.dp(this, 135));
        detail.setPadding(theme.dp(this, 8), 0, 0, 0);
        line.addView(detail);
        TextView arrow = label("›", 24, theme.muted);
        arrow.setPadding(theme.dp(this, 8), 0, 0, 0);
        line.addView(arrow);
        line.setBackgroundDrawable(theme.rowSelection(this, false));
        line.setFocusable(true);
        line.setOnClickListener(click);
    }
    private void toggle(String title, int icon, final String key, boolean fallback) {
        final LinearLayout line = line(icon);
        TextView name = label(title, 16, theme.ink);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        line.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        final SettingsToggle control = new SettingsToggle(this, theme);
        control.setContentDescription(title);
        control.setChecked(prefs.getBoolean(key, fallback));
        control.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                boolean checked = !control.isChecked();
                if (key.equals("dark")) changeTheme(view, null, Boolean.valueOf(checked));
                else {
                    control.setChecked(checked);
                    prefs.edit().putBoolean(key, checked).commit();
                }
            }
        });
        line.addView(control);
        line.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { control.performClick(); }
        });
        line.setFocusable(false);
    }
    private void changeTheme(View origin, String style, Boolean dark) {
        if (style != null && style.equals(prefs.getString("theme", "material"))) return;
        ThemeReveal reveal = ThemeReveal.capture(this, origin);
        int position = scroll.getScrollY();
        SharedPreferences.Editor editor = prefs.edit();
        if (style != null) editor.putString("theme", style);
        if (dark != null) editor.putBoolean("dark", dark.booleanValue());
        editor.commit();
        build();
        final int savedPosition = position;
        scroll.post(new Runnable() { @Override public void run() { scroll.scrollTo(0, savedPosition); } });
        if (reveal != null) reveal.show(this);
    }
    private void edit(final String title, final int icon, final String key, final String fallback, final boolean multiline) {
        String value = prefs.getString(key, fallback);
        row(title, icon, key.equals("registration_proxy") ? (value.length() == 0 ? getString(R.string.off) : getString(R.string.configured)) : (value.length() == 0 ? getString(R.string.none) : value), new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (TunnelState.active()) {
                    Toast.makeText(SettingsActivity.this, getString(R.string.disconnect_first), Toast.LENGTH_LONG).show();
                    return;
                }
                final EditText input = new EditText(SettingsActivity.this);
                input.setSingleLine(!multiline);
                input.setTextColor(theme.ink);
                input.setHintTextColor(theme.muted);
                input.setTextSize(16);
                input.setPadding(theme.dp(SettingsActivity.this, 14), theme.dp(SettingsActivity.this, 12), theme.dp(SettingsActivity.this, 14), theme.dp(SettingsActivity.this, 12));
                input.setBackgroundDrawable(theme.plate(SettingsActivity.this, theme.background));
                if (multiline) { input.setMinLines(3); input.setGravity(android.view.Gravity.TOP); }
                if (key.equals("dns")) input.setInputType(InputType.TYPE_CLASS_PHONE);
                if (key.equals("registration_proxy")) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setText(prefs.getString(key, fallback));
                final ShiroDialog dialog = new ShiroDialog(SettingsActivity.this, title, R.drawable.sticker_input);
                dialog.field(input);
                dialog.cancelButton();
                dialog.button(getString(R.string.save), new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String value = input.getText().toString().trim();
                        String direct = key.equals("direct") ? value : prefs.getString("direct", "");
                        String block = key.equals("block") ? value : prefs.getString("block", "");
                        String dns = key.equals("dns") ? value : prefs.getString("dns", "1.1.1.1");
                        try {
                            if (!key.equals("registration_proxy")) { Native.routes(direct, block); Native.validateDns(dns, direct, block); }
                            prefs.edit().putString(key, value).commit(); dialog.dismiss(); build();
                        } catch (IOException e) { input.setError(ErrorText.format(SettingsActivity.this, e.getMessage())); }
                    }
                });
                dialog.show();
            }
        });
    }
}
