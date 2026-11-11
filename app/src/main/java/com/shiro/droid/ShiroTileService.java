package com.shiro.droid;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

@TargetApi(24)
public final class ShiroTileService extends TileService {
    private boolean listening;
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override public void onStartListening() {
        super.onStartListening();
        if (!listening) {
            IntentFilter filter = new IntentFilter(ShiroVpnService.STATE);
            if (Build.VERSION.SDK_INT >= 33) Api33.register(this, changes, filter);
            else registerReceiver(changes, filter);
            listening = true;
        }
        refresh();
    }
    @Override public void onStopListening() {
        if (listening) {
            unregisterReceiver(changes);
            listening = false;
        }
        super.onStopListening();
    }
    @Override public void onClick() {
        super.onClick();
        if (Registration.running()) {
            Registration.cancel(this);
        } else if (TunnelState.active()) {
            startTunnel(ShiroVpnService.DISCONNECT);
        } else if (!Account.exists(this) || VpnConsent.prepare(this) != null) {
            openApp();
        } else {
            startTunnel(ShiroVpnService.CONNECT);
        }
    }
    private void startTunnel(String action) {
        Intent intent = new Intent(this, ShiroVpnService.class).setAction(action);
        try {
            if (Build.VERSION.SDK_INT >= 26 && ShiroVpnService.CONNECT.equals(action)) startForegroundService(intent);
            else startService(intent);
        } catch (IllegalStateException e) {
            Log.e("shiro", "quick settings could not start VPN service", e);
            Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            Log.e("shiro", "quick settings cannot start VPN service", e);
            Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_LONG).show();
        }
    }
    private void refresh() {
        Tile tile = getQsTile();
        if (tile == null) return;
        String state = TunnelState.state();
        tile.setState(TunnelState.active() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Shiro");
        if (Build.VERSION.SDK_INT >= 29) Api29.subtitle(tile, getString(statusText(state)));
        tile.updateTile();
    }
    static int statusText(String state) {
        if ("up".equals(state)) return R.string.connected;
        if ("registering".equals(state)) return R.string.registering;
        if ("connecting".equals(state)) return R.string.connecting;
        if ("failed".equals(state)) return R.string.connection_failed;
        return R.string.disconnected;
    }
    private void openApp() {
        Intent home = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) Api34.open(this, home);
        else startActivityAndCollapse(home);
    }
    @TargetApi(29)
    private static final class Api29 {
        static void subtitle(Tile tile, String text) { tile.setSubtitle(text); }
    }
    @TargetApi(33)
    private static final class Api33 {
        static void register(Context context, BroadcastReceiver receiver, IntentFilter filter) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }
    @TargetApi(34)
    private static final class Api34 {
        static void open(ShiroTileService service, Intent home) {
            PendingIntent pending = PendingIntent.getActivity(service, 0, home,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            service.startActivityAndCollapse(pending);
        }
    }
}
