package com.shiro.droid;

import java.io.IOException;

final class Native {
    static { System.loadLibrary("shiro"); }
    private Native() {}
    static native byte[] register(byte[] roots, String proxy) throws IOException;
    static native String address(byte[] account) throws IOException;
    static native String[] routes(String direct, String block) throws IOException;
    static native int rootOpen(String path, String address, String dns, String direct, String block) throws IOException;
    static native boolean rootPing(String path);
    static native boolean rootProtect(String path, int fd);
    static native void closeFd(int fd);
    static native long start(byte[] account, int tun, String direct, String block, int attempt, Object service) throws IOException;
    static native void validateDns(String dns, String direct, String block) throws IOException;
    static native String reason(long handle);
    static native int state(long handle);
    static native long[] stats(long handle);
    static native void stop(long handle);
}
