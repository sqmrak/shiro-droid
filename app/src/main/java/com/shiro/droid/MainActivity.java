package com.shiro.droid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;

import android.widget.FrameLayout;
import android.content.res.Configuration;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int CONSENT = 1;
    private Appearance theme;
    private TextView status, detail;
    private android.graphics.drawable.Drawable arrowDown, arrowUp;
    private Power power;
    private BroadcastReceiver events;
    private boolean resumed;
    private final Handler ticker = new Handler();
    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); ticker.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle state) {
        setTheme(getSharedPreferences("shiro", 0).getBoolean("dark", false)
                ? R.style.ShiroDark : R.style.ShiroLight);
        super.onCreate(state);
        events = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent event) { refresh(); finishRegistration(); }
        };
    }
    @Override protected void onStart() {
        super.onStart();
        registerReceiver(events, new IntentFilter("com.shiro.droid.STATE"));
        build();
        ticker.post(tick);
    }
    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        finishRegistration();
    }
    @Override protected void onPause() { resumed = false; super.onPause(); }
    @Override protected void onStop() { ticker.removeCallbacks(tick); power.stopMotion(); unregisterReceiver(events); super.onStop(); }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/Rubik-Regular.ttf"));
        view.setTextColor(color);
        return view;
    }
    private void build() {
        theme = new Appearance(this);
        SystemBars.apply(this, theme.background, theme.background);
        HomeLayout page = new HomeLayout(this);
        page.setBackgroundColor(theme.background);
        FrameLayout header = new FrameLayout(this);
        header.setMinimumHeight(theme.dp(this, 64));
        ImageView settings = headerButton(R.drawable.material_glyph_gear, getString(R.string.settings));
        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        });
        header.addView(settings, new FrameLayout.LayoutParams(theme.dp(this, 48), theme.dp(this, 48), Gravity.LEFT | Gravity.TOP));
        ImageView statsButton = headerButton(R.drawable.material_glyph_chart, getString(R.string.statistics));
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showStatistics(); }
        });
        header.addView(statsButton, new FrameLayout.LayoutParams(theme.dp(this, 48), theme.dp(this, 48), Gravity.RIGHT | Gravity.TOP));
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = theme.holo ? text("shiro-droid", 34, theme.ink) : new ShimmerText(this, theme.dark);
        title.setText("shiro-droid");
        title.setTextSize(34);
        title.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/Rubik-Bold.ttf"));
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleGroup.addView(title);
        TextView credit = text("by sqmrak :3", 12, theme.muted);
        credit.setGravity(Gravity.CENTER);
        titleGroup.addView(credit);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        titleParams.leftMargin = theme.dp(this, 56);
        titleParams.rightMargin = theme.dp(this, 56);
        header.addView(titleGroup, titleParams);
        page.header = header;
        page.addView(header);
        power = new Power(this);
        power.setContentDescription(getString(R.string.connect_disconnect));
        power.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { toggle(); }
        });
        page.power = power;
        page.addView(power);
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        page.info = info;
        page.addView(info);
        status = text("", 22, theme.ink);
        status.setGravity(Gravity.CENTER);
        status.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/Rubik-Bold.ttf"));
        info.addView(status, new LinearLayout.LayoutParams(-1, -2));
        detail = text("", 15, theme.muted);
        arrowDown = trafficArrow(R.drawable.shiro_arrow_downward);
        arrowUp = trafficArrow(R.drawable.shiro_arrow_upward);
        detail.setGravity(Gravity.CENTER);
        detail.setMinLines(1);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = theme.dp(this, 6);
        detailParams.bottomMargin = theme.dp(this, 12);
        info.addView(detail, detailParams);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(theme.dp(this, 12), theme.dp(this, 10), theme.dp(this, 12), theme.dp(this, 10));
        card.setBackgroundDrawable(theme.plate(this, theme.surface));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.cloudflare);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(logo, new LinearLayout.LayoutParams(theme.dp(this, 44), theme.dp(this, 44)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(theme.dp(this, 12), 0, theme.dp(this, 8), 0);
        card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = text("Cloudflare WARP", 17, theme.ink);
        name.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/Rubik-Bold.ttf"));
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);
        TextView endpoint = text(getString(R.string.automatic_endpoint), 13, theme.muted);
        endpoint.setSingleLine(true);
        endpoint.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(endpoint);
        info.addView(card, new LinearLayout.LayoutParams(-1, -2));
        TextView protocol = text("●  WIREGUARD  ·  WARP", 13, theme.muted);
        protocol.setPadding(theme.dp(this, 8), theme.dp(this, 10), 0, 0);
        info.addView(protocol);
        setContentView(page);
        int navigation = getResources().getConfiguration().navigation;
        android.app.UiModeManager modes = (android.app.UiModeManager)getSystemService(Context.UI_MODE_SERVICE);
        if ((modes != null && modes.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
                || navigation == Configuration.NAVIGATION_DPAD || navigation == Configuration.NAVIGATION_TRACKBALL) power.requestFocus();
    }
    private ImageView headerButton(int icon, String description) {
        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setColorFilter(theme.ink);
        int pad = theme.dp(this, 12);
        image.setPadding(pad, pad, pad, pad);
        image.setContentDescription(description);
        image.setFocusable(true);
        image.setBackgroundDrawable(theme.selection(this));
        return image;
    }
    private void showStatistics() {
        long[] stats = TunnelState.stats();
        String message = getString(R.string.no_connection);
        if (stats != null) {
            long seconds = stats[2] / 1000;
            message = getString(R.string.received) + ": " + bytes(stats[0]) + "\n"
                    + getString(R.string.sent) + ": " + bytes(stats[1]) + "\n"
                    + getString(R.string.uptime) + ": " + String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60) + "\n"
                    + getString(R.string.handshakes) + ": " + stats[3];
        }
        final ShiroDialog dialog = new ShiroDialog(this, getString(R.string.statistics), R.drawable.sticker_smile);
        dialog.message(message);
        dialog.button(getString(android.R.string.ok), new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        dialog.show();
    }
    private void refresh() {
        String state = TunnelState.state();
        String label = state.equals("up") ? getString(R.string.connected) : state.equals("registering") ? getString(R.string.registering) : state.equals("connecting") ? getString(R.string.connecting) : state.equals("failed") ? getString(R.string.connection_failed) : getString(R.string.disconnected);
        status.setText(label);
        long[] stats = TunnelState.stats();
        String message = displayError(this, TunnelState.error());
        if (state.equals("up") && stats != null) {
            String received = bytes(stats[0]), sent = bytes(stats[1]);
            android.text.SpannableString traffic = new android.text.SpannableString("↓ " + received + "    ↑ " + sent);
            traffic.setSpan(new android.text.style.ImageSpan(arrowDown, android.text.style.ImageSpan.ALIGN_BASELINE), 0, 1, 0);
            int up = traffic.toString().indexOf('↑');
            traffic.setSpan(new android.text.style.ImageSpan(arrowUp, android.text.style.ImageSpan.ALIGN_BASELINE), up, up + 1, 0);
            detail.setText(traffic);
            detail.setContentDescription(getString(R.string.received) + ": " + received + ", " + getString(R.string.sent) + ": " + sent);
        } else {
            detail.setText(message);
            detail.setContentDescription(null);
        }
        power.setState(state);
    }
    static String displayError(Context context, String error) {
        return ErrorText.format(context, error);
    }
    private android.graphics.drawable.Drawable trafficArrow(int resource) {
        android.graphics.drawable.Drawable icon = getResources().getDrawable(resource).mutate();
        icon.setColorFilter(theme.muted, android.graphics.PorterDuff.Mode.SRC_IN);
        int size = theme.dp(this, 17);
        icon.setBounds(0, 0, size, size);
        return icon;
    }
    private String bytes(long value) {
        if (value < 1024) return value + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        double n = value / 1024.0; int unit = 0;
        while (n >= 1024 && unit < units.length - 1) { n /= 1024; unit++; }
        return String.format(java.util.Locale.getDefault(), "%.1f %s", n, units[unit]);
    }
    private void toggle() {
        if (Registration.running()) {
            Registration.cancel(this);
        } else if (TunnelState.active()) {
            startServiceAction("com.shiro.droid.DISCONNECT");
        } else if (Build.VERSION.SDK_INT < 14) {
            startServiceAction("com.shiro.droid.CONNECT");
        } else if (!Account.exists(this)) {
            Registration.start(this);
        } else {
            requestVpn();
        }
    }
    private void finishRegistration() {
        if (resumed && Registration.takeReady()) requestVpn();
    }
    private void requestVpn() {
        Registration.takeReady();
        Intent permission = VpnConsent.prepare(this);
        if (permission == null) startServiceAction("com.shiro.droid.CONNECT");
        else startActivityForResult(permission, CONSENT);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CONSENT && result == RESULT_OK) startServiceAction("com.shiro.droid.CONNECT");
        else if (request == CONSENT) {
            TunnelState.set("failed", "VPN permission was refused");
            refresh();
        }
    }
    private void startServiceAction(String action) {
        Intent intent = new Intent();
        intent.setClassName(this, Build.VERSION.SDK_INT < 14 ? "com.shiro.droid.ShiroRootService" : "com.shiro.droid.ShiroVpnService");
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= 26 && action.endsWith("CONNECT") && !action.endsWith("DISCONNECT")) startForegroundService(intent);
        else startService(intent);
    }
    private final class Power extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String state;
        private float fill, press = 1f, phase;
        private Tween colorMotion, pressMotion, cycle;
        private final float[] previousShape = new float[PowerShape.POINTS];
        private float morph = 1f, bloom, fromBloom, targetBloom;
        private int faceColor, glyphColor, fromFace, fromGlyph, targetFace, targetGlyph;
        private float frontAngle, backAngle, frontSpeed, backSpeed, targetFrontSpeed, targetBackSpeed;
        private long motionStarted;
        private boolean shaking;
        private boolean visible = true;
        private final Path outline = new Path();
        private final RectF arc = new RectF();
        Power(Context context) { super(context); setClickable(true); setFocusable(true); }
        private float elapsed() {
            return (android.os.SystemClock.uptimeMillis() - motionStarted) / 1000f;
        }
        private float shapeAt(int index, float seconds) {
            float target = PowerShape.radius(index, state, Math.max(0f, seconds - .55f));
            return previousShape[index] + (target - previousShape[index]) * morph;
        }
        void setState(String next) {
            if (next.equals(state)) return;
            if (PowerShape.working(next) && PowerShape.working(state)) { state = next; return; }
            boolean initial = state == null;
            float seconds = initial ? 0f : elapsed();
            if (!initial) {
                for (int i = 0; i < previousShape.length; i++) previousShape[i] = shapeAt(i, seconds);
                frontAngle = PowerShape.angle(frontAngle, seconds, frontSpeed, targetFrontSpeed);
                backAngle = PowerShape.angle(backAngle, seconds, backSpeed, targetBackSpeed);
                frontSpeed = PowerShape.speed(seconds, frontSpeed, targetFrontSpeed);
                backSpeed = PowerShape.speed(seconds, backSpeed, targetBackSpeed);
            }
            if (colorMotion != null) colorMotion.cancel();
            if (cycle != null) { cycle.cancel(); cycle = null; }
            state = next;
            motionStarted = android.os.SystemClock.uptimeMillis();
            targetFrontSpeed = PowerShape.working(next) ? 72f : "up".equals(next) ? 30f : 0f;
            targetBackSpeed = PowerShape.working(next) ? -40f : "up".equals(next) ? -20f : 0f;
            if (initial || !Tween.enabled()) { frontSpeed = targetFrontSpeed; backSpeed = targetBackSpeed; }
            boolean failed = "failed".equals(next);
            targetFace = failed ? (theme.dark ? 0xff94000a : 0xffffd9d6)
                    : PowerShape.lit(next) ? theme.accent : theme.surface;
            targetGlyph = failed ? (theme.dark ? 0xffffffff : 0xff64131b)
                    : PowerShape.lit(next) ? theme.onAccent() : theme.muted;
            targetBloom = PowerShape.lit(next) ? 1f : 0f;
            float target = PowerShape.lit(next) ? 1f : 0f;
            if (initial || !visible || !Tween.enabled()) {
                for (int i = 0; i < previousShape.length; i++) previousShape[i] = PowerShape.radius(i, state, 0);
                morph = 1f; fill = target; bloom = targetBloom;
                faceColor = targetFace; glyphColor = targetGlyph;
                shaking = false;
            } else {
                final float fromFill = fill;
                fromFace = faceColor; fromGlyph = glyphColor; fromBloom = bloom;
                morph = 0f;
                shaking = failed;
                colorMotion = Tween.ofFloat(0f, 1f).setDuration(550).setInterpolator(Motion.SPRING)
                    .addUpdateListener(new Tween.Update() { public void onUpdate(Tween t) {
                        float value = (Float)t.getAnimatedValue();
                        morph = value;
                        float color = Motion.clamp(value);
                        fill = fromFill + (target - fromFill) * color;
                        bloom = fromBloom + (targetBloom - fromBloom) * color;
                        faceColor = Tween.mix(fromFace, targetFace, color);
                        glyphColor = Tween.mix(fromGlyph, targetGlyph, color);
                        invalidate();
                    }});
                colorMotion.start();
            }
            phase = 0f;
            if (visible && Tween.enabled() && PowerShape.lit(next)) {
                cycle = Tween.ofFloat(0f, 1f).setDuration(theme.holo ? (PowerShape.working(next) ? 1400 : 4500) : 48000)
                    .setRepeatCount(Tween.INFINITE).addUpdateListener(new Tween.Update() {
                        public void onUpdate(Tween t) { phase = (Float)t.getAnimatedValue(); invalidate(); }
                    });
                cycle.start();
            }
            invalidate();
        }
        void stopMotion() {
            visible = false;
            if (cycle != null) cycle.cancel();
            if (colorMotion != null) colorMotion.cancel();
            if (pressMotion != null) pressMotion.cancel();
        }
        private void pressTo(float target) {
            if (pressMotion != null) pressMotion.cancel();
            if (!visible || !Tween.enabled()) { press = target; invalidate(); return; }
            pressMotion = Tween.ofFloat(press, target).setDuration(target < 1f ? 120 : 500)
                .setInterpolator(target < 1f ? Motion.ENTER : Motion.SPRING)
                .addUpdateListener(new Tween.Update() { public void onUpdate(Tween t) {
                    press = (Float)t.getAnimatedValue(); invalidate();
                }});
            pressMotion.start();
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) pressTo(.90f);
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) pressTo(1f);
            return super.onTouchEvent(event);
        }
        @Override protected void onDraw(Canvas canvas) {
            float side = Math.min(getWidth(), getHeight());
            float x = getWidth() / 2f, y = getHeight() / 2f;
            float dot = Math.max(theme.dp(MainActivity.this, 4), side * .018f);
            float track = Math.max(0, side * .5f - dot - theme.dp(MainActivity.this, 3));
            float radius = track * .76f;
            boolean active = "registering".equals(state) || "connecting".equals(state) || "up".equals(state);
            int tint = "failed".equals(state) ? 0xffd64555 : theme.accent;
            canvas.save();
            canvas.scale(press, press, x, y);
            if (!theme.holo) {
                float seconds = elapsed();
                float front = PowerShape.angle(frontAngle, seconds, frontSpeed, targetFrontSpeed);
                float back = PowerShape.angle(backAngle, seconds, backSpeed, targetBackSpeed);
                if (shaking && seconds < .5f) front += (float)(9.2 * Math.sin(seconds * 36) * (1 - seconds * 2));
                if (bloom > 0f) {
                    canvas.save();
                    float scale = .6f + .4f * bloom;
                    canvas.scale(scale, scale, x, y);
                    canvas.rotate(back, x, y);
                    outline.reset();
                    for (int i = 0; i < PowerShape.POINTS; i++) {
                        double a = Math.PI * 2 * i / PowerShape.POINTS;
                        float r = side * .46f * PowerShape.wave(a, 6, .07f);
                        float px = x + r * (float)Math.sin(a), py = y - r * (float)Math.cos(a);
                        if (i == 0) outline.moveTo(px, py); else outline.lineTo(px, py);
                    }
                    outline.close();
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Tween.mix(theme.background, theme.accent, .23f));
                    paint.setAlpha((int)(255 * Motion.clamp(bloom)));
                    canvas.drawPath(outline, paint);
                    paint.setAlpha(255);
                    canvas.restore();
                }
                canvas.save();
                canvas.rotate(front, x, y);
                outline.reset();
                float face = side * .35f;
                for (int i = 0; i < PowerShape.POINTS; i++) {
                    double a = Math.PI * 2 * i / PowerShape.POINTS;
                    float r = face * shapeAt(i, seconds);
                    float px = x + r * (float)Math.sin(a), py = y - r * (float)Math.cos(a);
                    if (i == 0) outline.moveTo(px, py); else outline.lineTo(px, py);
                }
                outline.close();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(faceColor);
                canvas.drawPath(outline, paint);
                canvas.restore();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(theme.dp(MainActivity.this, 3), face * .11f));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(glyphColor);
                float g = face * .4f;
                arc.set(x-g, y-g, x+g, y+g);
                canvas.drawArc(arc, -54, 288, false, paint);
                canvas.drawLine(x, y-g*1.25f, x, y-g*.25f, paint);
                if (isFocused()) {
                    paint.setColor(theme.accent);
                    paint.setStrokeWidth(theme.dp(MainActivity.this, 3));
                    canvas.drawCircle(x, y, track, paint);
                }
                paint.setStrokeCap(Paint.Cap.BUTT);
                canvas.restore();
                return;
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(theme.dp(MainActivity.this, isFocused() ? 3 : 1));
            paint.setColor(isFocused() ? tint : (tint & 0x00ffffff) | 0x38000000);
            canvas.drawCircle(x, y, track, paint);
            float depth = theme.holo ? 0f : ("up".equals(state) ? 0f : .05f);
            outline.reset();
            for (int i = 0; i <= 96; i++) {
                double a = 2 * Math.PI * i / 96;
                float r = radius * (1f + depth * (float)Math.cos(9 * a)) / (1f + depth);
                float px = x + r * (float)Math.cos(a), py = y + r * (float)Math.sin(a);
                if (i == 0) outline.moveTo(px, py); else outline.lineTo(px, py);
            }
            outline.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Tween.mix(theme.surface, tint, fill * .12f));
            canvas.drawPath(outline, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(theme.dp(MainActivity.this, 2.5f), side * .013f));
            paint.setColor(tint);
            canvas.drawPath(outline, paint);
            paint.setStrokeWidth(Math.max(theme.dp(MainActivity.this, 3), side * .018f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            float glyph = radius * .30f;
            arc.set(x-glyph, y-glyph, x+glyph, y+glyph);
            canvas.drawArc(arc, -48, 276, false, paint);
            canvas.drawLine(x, y-glyph*1.35f, x, y, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.FILL);
            if (active) {
                double angle = phase * Math.PI * 2;
                canvas.drawCircle(x + track * (float)Math.cos(angle), y + track * (float)Math.sin(angle), dot, paint);
            } else {
                paint.setColor((tint & 0x00ffffff) | 0x60000000);
                canvas.drawCircle(x + track, y, dot * .6f, paint);
            }
            canvas.restore();
        }
    }
}
