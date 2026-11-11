package com.shiro.droid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class SettingsToggle extends View {
    private final Appearance theme;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private boolean checked;
    private float position;
    private Tween slide;

    SettingsToggle(Context context, Appearance theme) {
        super(context);
        this.theme = theme;
        setClickable(true);
        setFocusable(true);
    }
    boolean isChecked() { return checked; }
    void setChecked(boolean value) {
        checked = value;
        if (slide != null) slide.cancel();
        if (getWindowToken() == null || !Tween.enabled()) {
            position = value ? 1f : 0f;
            invalidate();
            return;
        }
        slide = Tween.ofFloat(position, value ? 1f : 0f).setDuration(theme.holo ? 170 : Motion.SETTLE_MS)
                .setInterpolator(theme.holo ? Motion.SWEEP : Motion.SETTLE)
                .addUpdateListener(new Tween.Update() {
                    @Override public void onUpdate(Tween tween) {
                        position = ((Float)tween.getAnimatedValue()).floatValue();
                        invalidate();
                    }
                });
        slide.start();
    }
    @Override protected void onDetachedFromWindow() {
        if (slide != null) { slide.cancel(); slide = null; }
        super.onDetachedFromWindow();
    }
    @Override protected void onMeasure(int width, int height) {
        setMeasuredDimension(theme.dp(getContext(), 50), theme.dp(getContext(), 48));
    }
    @Override protected void onDraw(Canvas canvas) {
        float top = (getHeight() - theme.dp(getContext(), 24)) / 2f;
        track.set(theme.dp(getContext(), 1), top, getWidth() - theme.dp(getContext(), 1), top + theme.dp(getContext(), 24));
        float radius = track.height() / 2f;
        paint.setColor(Tween.mix(theme.dark ? 0xff65575c : 0xffcdbfc4, theme.accent, Motion.clamp(position)));
        canvas.drawRoundRect(track, radius, radius, paint);
        paint.setColor(0xffffffff);
        float x = track.left + radius + (track.width() - 2 * radius) * Motion.clamp(position);
        canvas.drawCircle(x, track.centerY(), radius - theme.dp(getContext(), 2), paint);
        if (isFocused()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(theme.dp(getContext(), 2));
            paint.setColor(theme.ink);
            track.inset(theme.dp(getContext(), 1), -theme.dp(getContext(), 4));
            canvas.drawRoundRect(track, radius + theme.dp(getContext(), 4), radius + theme.dp(getContext(), 4), paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
    @android.annotation.TargetApi(14)
    @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Switch");
        info.setCheckable(true);
        info.setChecked(checked);
    }
    @android.annotation.TargetApi(14)
    @Override public void onInitializeAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName("android.widget.Switch");
        event.setChecked(checked);
    }
}
