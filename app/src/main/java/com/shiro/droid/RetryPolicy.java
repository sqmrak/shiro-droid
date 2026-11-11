package com.shiro.droid;

final class RetryPolicy {
    private static final int MAX_ATTEMPTS = 5;
    private RetryPolicy() {}
    static int next(int attempt, boolean wasUp, boolean reconnect, boolean failover) {
        if (!reconnect) return -1;
        int next = wasUp ? 0 : attempt + 1;
        return next < (failover ? MAX_ATTEMPTS : 1) ? next : -1;
    }
}
