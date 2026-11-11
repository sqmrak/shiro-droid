package com.shiro.droid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

final class ThemeReveal extends View {
    private final Bitmap picture;
    private final float x, y, reach;
    private final Path hole = new Path();
    private float radius;
    private Tween animation;

    private ThemeReveal(Activity activity, Bitmap picture, float x, float y) {
        super(activity);
        this.picture = picture;
        this.x = x;
        this.y = y;
        reach = (float)Math.hypot(Math.max(x, picture.getWidth() - x), Math.max(y, picture.getHeight() - y));
        setLayerTypeCompat();
        setClickable(true);
    }
    private void setLayerTypeCompat() {
        if (android.os.Build.VERSION.SDK_INT >= 11) setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    static ThemeReveal capture(Activity activity, View origin) {
        if (!Tween.enabled()) return null;
        View decor = activity.getWindow().getDecorView();
        int width = decor.getWidth(), height = decor.getHeight();
        if (width <= 0 || height <= 0) return null;
        try {
            Bitmap picture = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            decor.draw(new Canvas(picture));
            int[] a = new int[2], b = new int[2];
            origin.getLocationOnScreen(a);
            decor.getLocationOnScreen(b);
            return new ThemeReveal(activity, picture, a[0] - b[0] + origin.getWidth() / 2f,
                    a[1] - b[1] + origin.getHeight() / 2f);
        } catch (OutOfMemoryError error) {
            return null;
        }
    }
    void show(Activity activity) {
        final ViewGroup decor = (ViewGroup)activity.getWindow().getDecorView();
        decor.addView(this, new FrameLayout.LayoutParams(-1, -1));
        animation = Tween.ofFloat(0f, reach).setDuration(600)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .addUpdateListener(new Tween.Update() {
                    @Override public void onUpdate(Tween tween) {
                        radius = ((Float)tween.getAnimatedValue()).floatValue();
                        invalidate();
                    }
                }).addListener(new Tween.End() {
                    @Override public void onEnd(Tween tween, boolean cancelled) {
                        if (getParent() == decor) decor.removeView(ThemeReveal.this);
                    }
                });
        animation.start();
    }
    @Override protected void onDraw(Canvas canvas) {
        hole.reset();
        hole.addCircle(x, y, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(hole, Region.Op.DIFFERENCE);
        canvas.drawBitmap(picture, 0, 0, null);
        canvas.restore();
    }
    @Override protected void onDetachedFromWindow() {
        if (animation != null) { Tween old = animation; animation = null; old.cancel(); }
        super.onDetachedFromWindow();
    }
}
