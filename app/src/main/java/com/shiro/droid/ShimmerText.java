package com.shiro.droid;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.view.View;
import android.widget.TextView;

final class ShimmerText extends TextView {
    private final Matrix matrix = new Matrix();
    private final int[] colors;
    private LinearGradient shader;
    private int width;
    private float shift;
    private Tween drift;

    ShimmerText(Context context, boolean dark) {
        super(context);
        colors = dark ? new int[]{0xffffb1c7, 0xffff7da8, 0xffd999d6, 0xffffb1c7}
                : new int[]{0xffbd0f52, 0xffe03873, 0xff91387a, 0xffbd0f52};
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) { width = w; shader = new LinearGradient(0, 0, w, 0, colors, null, Shader.TileMode.REPEAT); }
    }
    private void syncMotion() {
        if (getWindowToken() == null || getWindowVisibility() != View.VISIBLE || !Tween.enabled()) {
            if (drift != null) { drift.cancel(); drift = null; }
            return;
        }
        if (drift != null) return;
        drift = Tween.ofFloat(0, 1).setDuration(5000).setRepeatCount(Tween.INFINITE)
                .addUpdateListener(new Tween.Update() { public void onUpdate(Tween value) {
                    shift = (Float)value.getAnimatedValue(); invalidate();
                }});
        drift.start();
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); syncMotion(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); syncMotion(); }
    @Override protected void onDetachedFromWindow() { if (drift != null) { drift.cancel(); drift = null; } super.onDetachedFromWindow(); }
    @Override protected void onDraw(android.graphics.Canvas canvas) {
        if (shader != null) { matrix.setTranslate(-shift * width, 0); shader.setLocalMatrix(matrix); getPaint().setShader(shader); }
        super.onDraw(canvas);
    }
}
