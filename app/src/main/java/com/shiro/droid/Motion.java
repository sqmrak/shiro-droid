package com.shiro.droid;

import android.view.animation.Interpolator;

/**
 * exteria editor curves and the material 3 emphasized pair, as plain interpolators that
 * {@link Tween} and view animators both accept
 */
final class Motion {
    private Motion() {
    }

    /** ActionBarLayout preview of exteria: stiffness 650, damping ratio 0.6, 480 ms, one clear wobble */
    static final long SPRING_MS = 480;
    static final Interpolator SPRING = spring(650f, 0.6f, SPRING_MS / 1000f);

    /** a livelier spring for things that pop, such as a sticker or a released button: two visible wobbles */
    static final long WOBBLE_MS = 560;
    static final Interpolator WOBBLE = spring(520f, 0.38f, WOBBLE_MS / 1000f);

    /** a thumb that settles with a barely visible overshoot, so it stays inside its track */
    static final long SETTLE_MS = 380;
    static final Interpolator SETTLE = spring(700f, 0.72f, SETTLE_MS / 1000f);

    /** EASE_OUT_QUINT, how screens and groups arrive, 320 ms */
    static final long ENTER_MS = 320;
    static final Interpolator ENTER = bezier(0.23f, 1f, 0.32f, 1f);

    /** EASE_IN, how things leave, 180 ms */
    static final long EXIT_MS = 180;
    static final Interpolator EXIT = bezier(0.42f, 0f, 1f, 1f);

    /** the emphasized decelerate and accelerate of material 3, for popups: 340 ms in, 200 ms out */
    static final long POPUP_ENTER_MS = 340;
    static final long POPUP_EXIT_MS = 200;
    static final Interpolator POPUP_ENTER = bezier(0.05f, 0.7f, 0.1f, 1f);
    static final Interpolator POPUP_EXIT = bezier(0.3f, 0f, 0.8f, 0.15f);

    /** rows that move or arrive in a list, 250 ms */
    static final long LIST_MS = 250;
    static final Interpolator LIST_MOVE = bezier(0.19919473f, 0.010644531f, 0.27920938f, 0.9102539f);

    /** a card that goes away starts leaving at once, 200 ms */
    static final long DISMISS_MS = 200;
    static final Interpolator DISMISS = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            float rest = 1f - clamp(t);
            return 1f - rest * rest;
        }
    };

    /** sweeps of a chart and the like: a gentle cubic deceleration */
    static final Interpolator SWEEP = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            float rest = 1f - clamp(t);
            return 1f - rest * rest * rest;
        }
    };

    /** a curve whose value passes 1 on its way, so a fade driven by it has to be clamped or run apart */
    static boolean overshoots(Interpolator curve) {
        return curve == SPRING || curve == WOBBLE || curve == SETTLE;
    }

    static float clamp(float t) {
        return t < 0f ? 0f : (t > 1f ? 1f : t);
    }

    /**
     * closed form of a damped spring, stretched to end at exactly 1 as exteria does; damping below 1
     * makes it overshoot
     */
    static Interpolator spring(float stiffness, final float damping, final float seconds) {
        final double frequency = Math.sqrt(stiffness);
        final double decay = frequency * damping;
        final double oscillation = frequency * Math.sqrt(1.0 - damping * damping);
        final double end = springAt(decay, oscillation, seconds);
        return new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                if (t <= 0f) return 0f;
                if (t >= 1f) return 1f;
                return (float) (springAt(decay, oscillation, t * seconds) / end);
            }
        };
    }

    private static double springAt(double decay, double oscillation, double seconds) {
        return 1.0 - Math.exp(-decay * seconds)
                * (Math.cos(oscillation * seconds) + decay / oscillation * Math.sin(oscillation * seconds));
    }

    private static final int TABLE = 256;

    /** cubic bezier as in css, sampled into a table once so a frame costs a lookup, not a root search */
    static Interpolator bezier(float x1, float y1, float x2, float y2) {
        final float[] y = new float[TABLE + 1];
        for (int i = 0; i <= TABLE; ++i) {
            float time = i / (float) TABLE;
            float low = 0f;
            float high = 1f;
            for (int step = 0; step < 24; ++step) {
                float middle = (low + high) * 0.5f;
                if (cubic(middle, x1, x2) < time) low = middle;
                else high = middle;
            }
            y[i] = cubic((low + high) * 0.5f, y1, y2);
        }
        y[0] = 0f;
        y[TABLE] = 1f;
        return new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                float at = clamp(t) * TABLE;
                int i = (int) at;
                if (i >= TABLE) return 1f;
                return y[i] + (y[i + 1] - y[i]) * (at - i);
            }
        };
    }

    private static float cubic(float t, float first, float second) {
        float inverse = 1f - t;
        return 3f * inverse * inverse * t * first + 3f * inverse * t * t * second + t * t * t;
    }
}
