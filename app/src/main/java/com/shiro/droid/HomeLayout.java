package com.shiro.droid;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

final class HomeLayout extends ViewGroup {
    View header, power, info;
    private boolean landscape;
    private final float density;
    private boolean tablet;
    private int contentLeft, contentWidth;

    HomeLayout(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
    }
    private int dp(float value) { return (int)(value * density + .5f); }
    private int margin(int width) { return dp(tablet ? 28 : width <= dp(320) ? 14 : 18); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        tablet = Math.min(width, height) >= dp(600);
        int inner = Math.max(0, Math.min(width - 2 * margin(width), dp(920)));
        contentWidth = inner;
        contentLeft = (width - inner) / 2;
        int headerWidth = Math.max(0, width - 2 * margin(width));
        header.measure(MeasureSpec.makeMeasureSpec(headerWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int available = Math.max(0, height - dp(10) - header.getMeasuredHeight() - dp(tablet ? 28 : 16));
        landscape = width > height && (tablet || height < dp(560));
        int infoWidth, side;
        if (landscape) {
            int left = (int)(inner * .44f);
            infoWidth = Math.max(0, Math.min(dp(tablet ? 400 : 340), inner - left - dp(14)));
            side = Math.min(available, left);
        } else {
            infoWidth = Math.min(dp(tablet ? 480 : 440), inner);
            side = (int)(infoWidth * (tablet ? .74f : .90f));
        }
        info.measure(MeasureSpec.makeMeasureSpec(infoWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        if (!landscape) side = Math.min(side, available - info.getMeasuredHeight() - dp(8));
        side = Math.max(0, Math.min(side, dp(tablet ? 400 : 350)));
        power.measure(MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l, height = b - t, margin = contentLeft, top = dp(10);
        int headerLeft = (width - header.getMeasuredWidth()) / 2;
        header.layout(headerLeft, top, headerLeft + header.getMeasuredWidth(), top + header.getMeasuredHeight());
        int below = top + header.getMeasuredHeight();
        int available = height - below - dp(tablet ? 28 : 16);
        int side = power.getMeasuredWidth();
        if (landscape) {
            int left = (int)(contentWidth * .44f);
            int x = margin + (left - side) / 2, y = below + Math.max(0, (available - side) / 2);
            power.layout(x, y, x + side, y + side);
            int start = margin + left + dp(14);
            x = start + (width - margin - start - info.getMeasuredWidth()) / 2;
            y = below + Math.max(0, (available - info.getMeasuredHeight()) / 2);
            info.layout(x, y, x + info.getMeasuredWidth(), y + info.getMeasuredHeight());
        } else {
            int x = (width - info.getMeasuredWidth()) / 2;
            int y = Math.max(below, height - dp(tablet ? 28 : 16) - info.getMeasuredHeight());
            info.layout(x, y, x + info.getMeasuredWidth(), y + info.getMeasuredHeight());
            int px = (width - side) / 2, py = below + Math.max(0, (y - dp(8) - below - side) / 2);
            power.layout(px, py, px + side, py + side);
        }
    }
}
