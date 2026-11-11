package com.shiro.droid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.ImageView;

final class AvatarView extends ImageView {
    private final Path clip = new Path();
    AvatarView(Context context) { super(context); }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        clip.reset();
        float radius = Math.min(w, h) * .3f;
        clip.addRoundRect(new RectF(0, 0, w, h), radius, radius, Path.Direction.CW);
    }
    @Override protected void onDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(clip);
        super.onDraw(canvas);
        canvas.restoreToCount(save);
    }
}
