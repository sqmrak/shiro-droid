package com.shiro.droid;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * root half of the app on android 2.3 to 3.x, which has no VpnService. its stdin stays open while the
 * app lives, so the helper drops its routes and dns redirect when the app dies
 */
final class RootHelper {
    private static final String TAG = "shiro";
    private static final String LIBRARY = "libshirorootd.so";
    private static final long READY_TIMEOUT_MS = 60000;
    private static final String[] SU_PATHS = {
        "su", "/system/xbin/su", "/system/bin/su", "/sbin/su",
        "/su/bin/su", "/system/bin/failsafe/su"
    };

    private static Process process;

    private RootHelper() {}

    /** whether this android needs the helper: VpnService exists from 4.0 */
    static boolean needed() {
        return android.os.Build.VERSION.SDK_INT < 14;
    }

    static synchronized boolean running(Context context) {
        if (process == null) return false;
        try {
            process.exitValue();
            process = null;
            return false;
        } catch (IllegalThreadStateException alive) {
            return Native.rootPing(new File(context.getFilesDir(), "rootd.sock").getAbsolutePath());
        }
    }

    /**
     * starts the helper through su unless it runs. blocks until it listens, so keep it off the main
     * thread; the IOException tells the user what to fix
     */
    static synchronized void start(Context context) throws IOException {
        if (running(context)) return;
        if (process != null) throw new IOException("the previous root helper is still cleaning up");
        File helper = new File(context.getApplicationInfo().nativeLibraryDir, LIBRARY);
        if (!helper.isFile()) throw new IOException("the root helper is not part of this build for this cpu");
        String socket = new File(context.getFilesDir(), "rootd.sock").getAbsolutePath();
        String line = "exec " + shellWord(helper.getAbsolutePath()) + " " + shellWord(socket) + " "
                + android.os.Process.myUid() + "\n";
        Process started = startSu(new SuLauncher() {
            @Override public Process launch(String path) throws IOException {
                return Runtime.getRuntime().exec(new String[] { path });
            }
        });
        try {
            OutputStream stdin = started.getOutputStream();
            stdin.write(line.getBytes("UTF-8"));
            stdin.flush();
            drain(started);
            awaitReady(started);
        } catch (IOException e) {
            started.destroy();
            throw e;
        }
        process = started;
    }

    interface SuLauncher {
        Process launch(String path) throws IOException;
    }

    static Process startSu(SuLauncher launcher) throws IOException {
        IOException last = null;
        for (String path : SU_PATHS) {
            try {
                return launcher.launch(path);
            } catch (IOException e) {
                last = e;
            }
        }
        throw new IOException("root access is needed on this android and su could not be started", last);
    }

    static synchronized void stop() {
        if (process == null) return;
        final Process stopping = process;
        try {
            stopping.getOutputStream().close();
        } catch (IOException e) {
            Log.w(TAG, "the root helper's pipe was already closed", e);
        }
        Thread waiter = new Thread(new Runnable() {
            @Override public void run() {
                try { stopping.waitFor(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "shiro-rootd-stop");
        waiter.setDaemon(true);
        waiter.start();
        try { waiter.join(5000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (waiter.isAlive()) Log.e(TAG, "the root helper did not finish cleanup");
        else process = null;
    }

    /** only the characters of an app path are allowed, so the line cannot become anything else */
    static String shellWord(String path) throws IOException {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            boolean plain = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '/' || c == '.' || c == '_' || c == '-';
            if (!plain) throw new IOException("the app path has a character the root helper cannot take: " + path);
        }
        return path;
    }

    private static void awaitReady(final Process started) throws IOException {
        final String[] outcome = new String[1];
        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader out = new BufferedReader(
                            new InputStreamReader(started.getInputStream(), "UTF-8"));
                    String first = out.readLine();
                    synchronized (outcome) {
                        outcome[0] = first == null ? "" : first;
                        outcome.notifyAll();
                    }
                    // later output is not expected, but a full pipe would stop the helper
                    while (out.readLine() != null) { /* discarded */ }
                } catch (IOException e) {
                    synchronized (outcome) {
                        if (outcome[0] == null) outcome[0] = "";
                        outcome.notifyAll();
                    }
                }
            }
        }, "shiro-rootd-out");
        reader.setDaemon(true);
        reader.start();
        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MS;
        synchronized (outcome) {
            while (outcome[0] == null) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) throw new IOException("root access was not granted in time");
                try {
                    outcome.wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted while waiting for root access");
                }
            }
        }
        if (!"READY".equals(outcome[0])) {
            throw new IOException("root access was refused or the helper could not start");
        }
    }

    private static void drain(final Process started) {
        Thread errors = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader err = new BufferedReader(
                            new InputStreamReader(started.getErrorStream(), "UTF-8"));
                    String line;
                    while ((line = err.readLine()) != null) Log.w(TAG, "shiro_rootd: " + line);
                } catch (IOException e) {
                    // the process ended, nothing is left to read
                }
            }
        }, "shiro-rootd-err");
        errors.setDaemon(true);
        errors.start();
    }
}
