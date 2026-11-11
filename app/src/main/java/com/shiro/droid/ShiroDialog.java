package com.shiro.droid;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class ShiroDialog extends Dialog {
    private final Appearance theme;
    private final LinearLayout body, buttons;
    private final FrameLayout root;
    private final DialogHolder holder;
    private final ScrollView scroll;
    private final TextView heading;
    private int bodyHeightLimit;
    private final Context context;
    private final View cardView, stickerView;

    ShiroDialog(Context context, String title, int sticker) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = context;
        theme = new Appearance(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = new FrameLayout(context);
        root.setBackgroundColor(0x99000000);
        root.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { cancel(); } });
        holder = new DialogHolder(context);
        FrameLayout.LayoutParams holderParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        holderParams.leftMargin = dp(20);
        holderParams.rightMargin = dp(20);
        root.addView(holder, holderParams);
        LinearLayout card = new LinearLayout(context);
        cardView = card;
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundDrawable(theme.plate(context, theme.surface));
        card.setClickable(true);
        holder.addView(card, new FrameLayout.LayoutParams(-1, -2));
        ImageView face = new ImageView(context);
        stickerView = face;
        face.setImageResource(sticker);
        face.setScaleType(ImageView.ScaleType.FIT_CENTER);
        holder.addView(face, new FrameLayout.LayoutParams(0, 0, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        heading = text(title, 19, theme.ink);
        heading.setGravity(Gravity.CENTER);
        card.addView(heading);
        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll = new ScrollView(context) {
            @Override protected void onMeasure(int w, int h) {
                int mode = MeasureSpec.getMode(h);
                int size = mode == MeasureSpec.UNSPECIFIED ? bodyHeightLimit : Math.min(bodyHeightLimit, MeasureSpec.getSize(h));
                super.onMeasure(w, MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST));
            }
        };
        scroll.addView(body);
        card.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        buttons = new LinearLayout(context);
        buttons.setPadding(0, dp(12), 0, 0);
        card.addView(buttons);
        setContentView(root);
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(-1, -1);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private final class DialogHolder extends FrameLayout {
        DialogHolder(Context context) { super(context); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = Math.min(dp(400), MeasureSpec.getSize(widthSpec));
            int height = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED
                    ? getResources().getDisplayMetrics().heightPixels : MeasureSpec.getSize(heightSpec);
            int sticker = Math.min(dp(height < dp(460) ? 64 : 100), Math.max(0, (height - dp(140)) / 2));
            int cardTop = sticker * 72 / 100;
            int paddingTop = sticker - cardTop + dp(20);
            FrameLayout.LayoutParams faceParams = (FrameLayout.LayoutParams)stickerView.getLayoutParams();
            faceParams.width = sticker;
            faceParams.height = sticker;
            FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams)cardView.getLayoutParams();
            cardParams.topMargin = cardTop;
            if (cardView.getPaddingTop() != paddingTop || cardView.getPaddingLeft() != dp(18))
                cardView.setPadding(dp(18), paddingTop, dp(18), dp(16));
            int inner = Math.max(0, width - dp(36));
            int innerSpec = MeasureSpec.makeMeasureSpec(inner, MeasureSpec.EXACTLY);
            int unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            heading.measure(innerSpec, unspecified);
            buttons.measure(innerSpec, unspecified);
            bodyHeightLimit = Math.max(0, height - cardTop - paddingTop - dp(16)
                    - heading.getMeasuredHeight() - buttons.getMeasuredHeight());
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightSpec);
        }
    }
    @Override protected void onStart() {
        super.onStart();
        if (Tween.enabled()) {
            ViewFx.enter(cardView, dp(18), 1f, 0, Motion.POPUP_ENTER_MS, Motion.POPUP_ENTER);
            ViewFx.enter(stickerView, 0f, .4f, 60, Motion.WOBBLE_MS, Motion.WOBBLE);
        }
    }
    private int dp(float n) { return theme.dp(context, n); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(context);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(android.graphics.Typeface.createFromAsset(context.getAssets(), "fonts/Rubik-Regular.ttf"));
        return view;
    }
    void message(String value) {
        TextView view = text(value, 15, theme.muted);
        view.setGravity(Gravity.CENTER); view.setPadding(0, dp(12), 0, dp(4));
        body.addView(view);
    }
    void field(EditText input) { body.addView(input, new LinearLayout.LayoutParams(-1, -2)); }
    void choice(String label, boolean selected, final Runnable action) {
        TextView item = text(label, 16, selected ? theme.accent : theme.ink);
        if (selected) {
            android.graphics.drawable.Drawable check = context.getResources().getDrawable(R.drawable.shiro_check).mutate();
            check.setColorFilter(theme.accent, android.graphics.PorterDuff.Mode.SRC_IN);
            check.setBounds(0, 0, dp(22), dp(22));
            item.setCompoundDrawables(null, null, check, null);
        }
        item.setGravity(Gravity.CENTER); item.setPadding(dp(8), dp(14), dp(8), dp(14));
        item.setFocusable(true);
        item.setBackgroundDrawable(theme.rowSelection(context, selected));
        item.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dismiss(); action.run(); } });
        body.addView(item, new LinearLayout.LayoutParams(-1, -2));
    }
    void button(String label, final View.OnClickListener listener) {
        TextView button = text(label, 15, theme.accent);
        button.setGravity(Gravity.CENTER); button.setFocusable(true);
        button.setBackgroundDrawable(theme.selection(context));
        button.setOnClickListener(listener);
        buttons.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
    }
    void cancelButton() { button(context.getString(android.R.string.cancel), new View.OnClickListener() { public void onClick(View v) { cancel(); } }); }
}
