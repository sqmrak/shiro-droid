package com.shiro.droid;

import android.app.Activity;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
public final class SystemBarsTest {
    @Test @Config(sdk = 28) public void lightAndDarkBarsFollowThePage() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        SystemBars.apply(activity, 0xfffff8f9, 0xfffff8f9);
        assertEquals(0xfffff8f9, activity.getWindow().getStatusBarColor());
        assertEquals(0xfffff8f9, activity.getWindow().getNavigationBarColor());
        int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertTrue((flags & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0);
        assertTrue((flags & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0);
        SystemBars.apply(activity, 0xff1a1113, 0xff1a1113);
        flags = activity.getWindow().getDecorView().getSystemUiVisibility();
        assertEquals(0, flags & (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
    }
    @Test @Config(sdk = 19) public void oldSystemDoesNotCallNewBarApis() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        SystemBars.apply(activity, 0xffffffff, 0xffffffff);
        assertEquals(0, activity.getWindow().getDecorView().getSystemUiVisibility());
    }
}
