package com.shiro.droid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Build.VERSION.SDK_INT < 14) return;
        if (!context.getSharedPreferences("shiro", 0).getBoolean("connect_at_startup", false)) return;
        if (VpnConsent.prepare(context) != null) {
            Log.w("shiro", "VPN permission is needed before startup connection");
            return;
        }
        Intent start = new Intent(context, ShiroVpnService.class).setAction(ShiroVpnService.CONNECT);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(start); else context.startService(start);
    }
}
