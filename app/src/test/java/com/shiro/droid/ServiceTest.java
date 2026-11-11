package com.shiro.droid;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = {19, 28})
public final class ServiceTest {
    @Test public void screensAndServiceCanBeCreated() {
        org.robolectric.android.controller.ActivityController<MainActivity> screen =
                Robolectric.buildActivity(MainActivity.class).create().start().resume();
        assertNotNull(screen.get());
        screen.pause().stop().destroy();
        ShiroVpnService service = Robolectric.buildService(ShiroVpnService.class).create().get();
        assertNotNull(service);
        service.onStartCommand(new Intent(), 0, 1);
        service.onDestroy();
        ShiroRootService rootService = Robolectric.buildService(ShiroRootService.class).create().get();
        assertNotNull(rootService);
        rootService.onDestroy();
    }
    @Test public void defaultsArePrivateAndBackupIsOff() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        assertFalse(context.getSharedPreferences("shiro", 0).getBoolean("connect_at_startup", false));
        assertEquals(0, context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP);
        assertTrue(Build.VERSION.SDK_INT >= 14);
    }
}
