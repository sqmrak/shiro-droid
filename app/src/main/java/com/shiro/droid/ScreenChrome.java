package com.shiro.droid;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class ScreenChrome {
    private ScreenChrome() {}
    static View wrap(final Activity activity, Appearance theme, String title, View body) {
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(theme.background);
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBaselineAligned(false);
        int color = theme.holo ? (theme.dark ? 0xff222222 : 0xffeeeeee) : (theme.dark ? 0xff271d20 : 0xffc10060);
        int ink = theme.holo && !theme.dark ? theme.ink : 0xffffffff;
        SystemBars.apply(activity, color, theme.background);
        bar.setBackgroundColor(color);
        TextView back = new TextView(activity);
        back.setText("‹"); back.setTextSize(32); back.setTextColor(ink); back.setGravity(Gravity.CENTER);
        back.setContentDescription(activity.getString(R.string.back));
        back.setFocusable(true);
        android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, new android.graphics.drawable.ColorDrawable(0x44808080));
        background.addState(new int[]{android.R.attr.state_pressed}, new android.graphics.drawable.ColorDrawable(0x44808080));
        background.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(0x00000000));
        back.setBackgroundDrawable(background);
        back.setOnClickListener(new View.OnClickListener() { public void onClick(View view) { activity.onBackPressed(); } });
        bar.addView(back, new LinearLayout.LayoutParams(theme.dp(activity, 48), theme.dp(activity, 52)));
        TextView heading = new TextView(activity);
        heading.setText(title); heading.setTextSize(18); heading.setTextColor(ink);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setSingleLine(true); heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.setTypeface(android.graphics.Typeface.createFromAsset(activity.getAssets(), "fonts/Rubik-Bold.ttf"));
        bar.addView(heading, new LinearLayout.LayoutParams(0, -1, 1));
        page.addView(bar, new LinearLayout.LayoutParams(-1, theme.dp(activity, 52)));
        page.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        return page;
    }
}
