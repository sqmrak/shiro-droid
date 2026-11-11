package com.shiro.droid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = {28, 34})
public final class ShiroTileServiceTest {
    @Test public void manifestRestrictsBindingToSystem() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ServiceInfo service = context.getPackageManager().getServiceInfo(
                new ComponentName(context, ShiroTileService.class), PackageManager.GET_META_DATA);
        assertTrue(service.exported);
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", service.permission);
        assertNotEquals(0, service.icon);
    }
    @Test public void activeTileRequestsDisconnect() {
        TunnelState.set("up", "");
        try {
            ShiroTileService tile = Robolectric.buildService(ShiroTileService.class).create().get();
            tile.onStartListening();
            tile.onClick();
            Intent started = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService();
            assertNotNull(started);
            assertEquals(ShiroVpnService.DISCONNECT, started.getAction());
            assertEquals(ShiroVpnService.class.getName(), started.getComponent().getClassName());
            tile.onStopListening();
        } finally {
            TunnelState.set("off", "");
        }
    }
    @Test public void subtitleCoversTunnelStates() {
        assertEquals(R.string.connecting, ShiroTileService.statusText("connecting"));
        assertEquals(R.string.registering, ShiroTileService.statusText("registering"));
        assertEquals(R.string.connected, ShiroTileService.statusText("up"));
        assertEquals(R.string.connection_failed, ShiroTileService.statusText("failed"));
        assertEquals(R.string.disconnected, ShiroTileService.statusText("off"));
    }
}
