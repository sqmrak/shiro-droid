package com.shiro.droid;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.IOException;
import java.util.Arrays;

@android.annotation.TargetApi(14)
public final class ShiroVpnService extends VpnService {
    public static final String CONNECT = "com.shiro.droid.CONNECT";
    public static final String DISCONNECT = "com.shiro.droid.DISCONNECT";
    public static final String STATE = "com.shiro.droid.STATE";
    private static final String TAG = "shiro";
    private static final int NOTICE = 1;
    private HandlerThread thread;
    private Handler worker;
    private volatile int generation;
    private volatile boolean wanted;
    private volatile boolean foreground;
    private ParcelFileDescriptor tun;
    private long core;
    private byte[] account;
    private int attempt;
    private boolean wasUp;
    private String network = "";
    private boolean networkOffline;
    private BroadcastReceiver networkReceiver;

    @Override public void onCreate() {
        super.onCreate();
        thread = new HandlerThread("shiro-vpn");
        thread.start();
        worker = new Handler(thread.getLooper());
        networkReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                ConnectivityManager manager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo info = manager == null ? null : manager.getActiveNetworkInfo();
                if (info == null || !info.isConnected()) { networkOffline = true; return; }
                String current = info.getType() + ":" + info.getSubtype() + ":" + info.getExtraInfo();
                if (network.length() != 0 && (networkOffline || !network.equals(current)) && wanted) {
                    final int run = ++generation;
                    worker.post(new Runnable() { @Override public void run() { reconnect(run); } });
                }
                network = current;
                networkOffline = false;
            }
        };
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (DISCONNECT.equals(action)) {
            wanted = false;
            final int run = ++generation;
            worker.post(new Runnable() { @Override public void run() { disconnect(run, "off", ""); } });
            return START_NOT_STICKY;
        }
        if (!CONNECT.equals(action)) return START_NOT_STICKY;
        if (wanted) return START_NOT_STICKY;
        wanted = true;
        publish("connecting", "");
        final int run = ++generation;
        startForeground(NOTICE, notice(getString(R.string.connecting)));
        foreground = true;
        worker.post(new Runnable() { @Override public void run() { connect(run); } });
        return START_NOT_STICKY;
    }

    @Override public void onRevoke() {
        wanted = false;
        final int run = ++generation;
        worker.post(new Runnable() { @Override public void run() { disconnect(run, "failed", "VPN permission was revoked"); } });
    }

    @Override public void onDestroy() {
        if (wanted) publish("failed", "VPN service stopped");
        wanted = false;
        ++generation;
        unregisterReceiver(networkReceiver);
        worker.post(new Runnable() {
            @Override public void run() {
                cleanup();
                thread.getLooper().quit();
            }
        });
        super.onDestroy();
    }

    private boolean live(int run) { return wanted && generation == run; }

    private void connect(final int run) {
        if (!live(run)) return;
        cleanup();
        SharedPreferences prefs = getSharedPreferences("shiro", 0);
        String direct = prefs.getString("direct", "");
        String block = prefs.getString("block", "");
        String dns = prefs.getString("dns", "1.1.1.1");
        try {
            String[] routes = Native.routes(direct, block);
            Native.validateDns(dns, direct, block);
            account = Account.loadOrRegister(this, prefs.getString("registration_proxy", ""), new Runnable() {
                @Override public void run() { if (live(run)) publish("registering", ""); }
            });
            if (!live(run)) { cleanup(); return; }
            Builder builder = new Builder().setSession("Shiro").setMtu(1280);
            builder.addAddress(Native.address(account), 32);
            builder.addAddress("fd00:5348::1", 128);
            builder.addRoute("::", 0);
            for (String route : routes) {
                int slash = route.indexOf('/');
                builder.addRoute(route.substring(0, slash), Integer.parseInt(route.substring(slash + 1)));
            }
            builder.addDnsServer(dns);
            tun = builder.establish();
            if (tun == null) throw new IOException("VPN permission was refused or revoked");
            if (!live(run)) { cleanup(); return; }
            attempt = 0;
            wasUp = false;
            startCore(run, direct, block);
        } catch (IOException e) {
            failed(run, e.getMessage());
        } catch (IllegalArgumentException e) {
            failed(run, "the system refused VPN settings: " + e.getMessage());
        } catch (SecurityException e) {
            failed(run, "VPN permission is missing: " + e.getMessage());
        } catch (IllegalStateException e) {
            failed(run, "VPN service is unavailable: " + e.getMessage());
        }
    }

    private void startCore(final int run, final String direct, final String block) throws IOException {
        if (!live(run)) { cleanup(); return; }
        publish("connecting", "");
        core = Native.start(account, tun.getFd(), direct, block, attempt, this);
        worker.postDelayed(new Runnable() {
            @Override public void run() { monitor(run, direct, block); }
        }, 500);
    }

    private void monitor(final int run, final String direct, final String block) {
        if (!live(run) || core == 0) return;
        int state = Native.state(core);
        if (state == 1) {
            TunnelState.updateStats(Native.stats(core));
            if (!wasUp) { wasUp = true; attempt = 0; publish("up", ""); }
        } else if (state == 2) {
            String reason = Native.reason(core);
            Native.stop(core);
            core = 0;
            SharedPreferences prefs = getSharedPreferences("shiro", 0);
            attempt = RetryPolicy.next(attempt, wasUp, prefs.getBoolean("auto_reconnect", true),
                                       prefs.getBoolean("failover", true));
            wasUp = false;
            if (attempt < 0) { failed(run, reason); return; }
            try { startCore(run, direct, block); } catch (IOException e) { failed(run, e.getMessage()); }
            return;
        }
        worker.postDelayed(new Runnable() { @Override public void run() { monitor(run, direct, block); } }, 500);
    }

    private void reconnect(int run) {
        if (!live(run)) return;
        cleanup();
        connect(run);
    }

    private void failed(int run, String message) {
        if (!live(run)) { cleanup(); return; }
        wanted = false;
        cleanup();
        publish("failed", message == null ? "connection failed" : message);
        foreground = false;
        stopForeground(true);
        stopSelf();
    }

    private void disconnect(int run, String state, String message) {
        if (run != generation) return;
        cleanup();
        publish(state, message);
        foreground = false;
        stopForeground(true);
        stopSelf();
    }

    private void cleanup() {
        if (core != 0) { Native.stop(core); core = 0; }
        if (tun != null) {
            try { tun.close(); } catch (IOException e) { Log.w(TAG, "cannot close tunnel", e); }
            tun = null;
        }
        if (account != null) { Arrays.fill(account, (byte)0); account = null; }
    }

    private void publish(String state, String error) {
        TunnelState.set(state, error);
        if (foreground && (state.equals("registering") || state.equals("connecting") || state.equals("up"))) {
            int label = state.equals("up") ? R.string.connected : state.equals("registering") ? R.string.registering : R.string.connecting;
            android.app.NotificationManager manager = (android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            manager.notify(NOTICE, notice(getString(label)));
        }
        Intent event = new Intent(STATE).setPackage(getPackageName());
        event.putExtra("state", state);
        event.putExtra("error", error);
        if (core != 0) event.putExtra("stats", Native.stats(core));
        sendBroadcast(event);
        Log.i(TAG, state + (error.length() == 0 ? "" : ": " + error));
    }

    private Notification notice(String text) {
        if (Build.VERSION.SDK_INT >= 26) NotificationChannelCompat.create(this);
        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= 0x04000000;
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, flags);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.notification_large))
                .setContentTitle("Shiro")
                .setContentText(text).setOngoing(true).setContentIntent(pending);
        if (Build.VERSION.SDK_INT >= 26) NotificationChannelCompat.setChannel(builder);
        return builder.getNotification();
    }
}
