package com.shiro.droid;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = 28)
public final class NotificationTest {
    @Test public void notificationTracksConnectionAndShowsAppIcon() throws Exception {
        ShiroVpnService service = Robolectric.buildService(ShiroVpnService.class).create().get();
        Field foreground = ShiroVpnService.class.getDeclaredField("foreground");
        foreground.setAccessible(true);
        foreground.setBoolean(service, true);
        Method publish = ShiroVpnService.class.getDeclaredMethod("publish", String.class, String.class);
        publish.setAccessible(true);
        NotificationManager manager = (NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            publish.invoke(service, "connecting", "");
            Notification first = Shadows.shadowOf(manager).getNotification(1);
            assertNotNull(first);
            assertEquals(service.getString(R.string.connecting), first.extras.getCharSequence(Notification.EXTRA_TEXT));
            assertEquals(R.mipmap.ic_launcher, first.getSmallIcon().getResId());
            assertNotNull(first.extras.getParcelable(Notification.EXTRA_LARGE_ICON));
            publish.invoke(service, "up", "");
            Notification connected = Shadows.shadowOf(manager).getNotification(1);
            assertEquals(service.getString(R.string.connected), connected.extras.getCharSequence(Notification.EXTRA_TEXT));
        } finally {
            service.onDestroy();
            TunnelState.set("off", "");
        }
    }
}
