package com.shiro.droid;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

final class ContentColumn extends FrameLayout {
    ContentColumn(Context context) { super(context); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (getChildCount() > 0) {
            LayoutParams params = (LayoutParams)getChildAt(0).getLayoutParams();
            params.width = Math.min(width, (int)(640 * getResources().getDisplayMetrics().density + .5f));
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
