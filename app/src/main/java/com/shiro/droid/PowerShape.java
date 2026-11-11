package com.shiro.droid;

final class PowerShape {
    static final int POINTS = 144;
    static final float TURN_SECONDS = .7f;
    private static final int[] WORKING_LOBES = {8, 5, 10, 6};
    private static final float[] WORKING_DEPTHS = {.11f, .14f, .09f, .14f};
    private static final int[] CONNECTED_LOBES = {12, 6, 8, 5};
    private static final float[] CONNECTED_DEPTHS = {.06f, .12f, .09f, .11f};
    private PowerShape() {}

    static boolean working(String state) {
        return "registering".equals(state) || "connecting".equals(state);
    }
    static boolean lit(String state) { return working(state) || "up".equals(state); }
    static float radius(int index, String state, float seconds) {
        double angle = Math.PI * 2 * index / POINTS;
        if (lit(state)) {
            int[] lobes = working(state) ? WORKING_LOBES : CONNECTED_LOBES;
            float[] depths = working(state) ? WORKING_DEPTHS : CONNECTED_DEPTHS;
            float duration = working(state) ? .7f : 1.6f;
            float cycle = Math.max(0, seconds) / duration % lobes.length;
            int from = (int)cycle;
            int to = (from + 1) % lobes.length;
            float t = cycle - from;
            t = t * t * (3f - 2f * t);
            float a = wave(angle, lobes[from], depths[from]);
            float b = wave(angle, lobes[to], depths[to]);
            return a + (b - a) * t;
        }
        return "failed".equals(state) ? wave(angle, 5, .09f) : wave(angle, 12, .045f);
    }
    static float speed(float seconds, float from, float to) {
        float t = Math.min(1f, Math.max(0f, seconds / TURN_SECONDS));
        return from + (to - from) * t * t * (3f - 2f * t);
    }
    static float angle(float base, float seconds, float from, float to) {
        float t = Math.min(1f, Math.max(0f, seconds / TURN_SECONDS));
        float blend = TURN_SECONDS * (t * t * t - .5f * t * t * t * t);
        return base + from * seconds + (to - from) * (blend + Math.max(0f, seconds - TURN_SECONDS));
    }
    static float wave(double angle, int lobes, float depth) {
        return 1f + depth * (float)Math.cos(lobes * angle);
    }
}
