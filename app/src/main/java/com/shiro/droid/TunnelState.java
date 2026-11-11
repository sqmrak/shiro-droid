package com.shiro.droid;

final class TunnelState {
    private static String state = "off";
    private static String error = "";
    private static long[] stats;
    private TunnelState() {}
    static synchronized void set(String value, String message) {
        state = value;
        error = message == null ? "" : message;
        if (!value.equals("up")) stats = null;
    }
    static synchronized void updateStats(long[] value) { stats = value == null ? null : value.clone(); }
    static synchronized long[] stats() { return stats == null ? null : stats.clone(); }
    static synchronized String state() { return state; }
    static synchronized String error() { return error; }
    static synchronized boolean active() {
        return state.equals("registering") || state.equals("connecting") || state.equals("up");
    }
}
