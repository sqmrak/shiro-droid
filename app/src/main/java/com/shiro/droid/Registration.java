package com.shiro.droid;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.Arrays;

final class Registration {
    interface Work { void run() throws IOException; }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean running;
    private static boolean ready;
    private static int generation;

    private Registration() {}

    static boolean running() { return running; }
    static boolean takeReady() {
        boolean result = ready;
        ready = false;
        return result;
    }
    static void start(Context context) {
        final Context app = context.getApplicationContext();
        final String proxy = app.getSharedPreferences("shiro", 0).getString("registration_proxy", "");
        begin(app, new Work() {
            @Override public void run() throws IOException {
                byte[] account = Account.loadOrRegister(app, proxy, null);
                Arrays.fill(account, (byte)0);
            }
        });
    }
    static Thread begin(Context context, final Work work) {
        if (running) return null;
        final Context app = context.getApplicationContext();
        final int run = ++generation;
        running = true;
        ready = false;
        publish(app, "registering", "");
        Thread worker = new Thread(new Runnable() {
            @Override public void run() {
                String message = null;
                try { work.run(); }
                catch (IOException e) { message = e.getMessage() == null ? "registration failed" : e.getMessage(); }
                final String error = message;
                MAIN.post(new Runnable() {
                    @Override public void run() {
                        if (generation != run || !running) return;
                        running = false;
                        ready = error == null;
                        publish(app, ready ? "off" : "failed", ready ? "" : error);
                    }
                });
            }
        }, "shiro-registration");
        worker.start();
        return worker;
    }
    static void cancel(Context context) {
        if (!running) return;
        ++generation;
        running = false;
        ready = false;
        publish(context.getApplicationContext(), "off", "");
    }
    private static void publish(Context context, String state, String error) {
        TunnelState.set(state, error);
        context.sendBroadcast(new Intent(ShiroVpnService.STATE).setPackage(context.getPackageName()));
    }
}
