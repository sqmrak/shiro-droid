package com.shiro.droid;

import android.content.Context;
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
public final class AppearanceTest {
    @Test public void themesAndDarkModeAreStored() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("shiro", 0).edit().clear().commit();
        Appearance material = new Appearance(context);
        assertFalse(material.holo);
        assertFalse(material.dark);
        context.getSharedPreferences("shiro", 0).edit().putString("theme", "holo").putBoolean("dark", true).commit();
        Appearance holo = new Appearance(context);
        assertTrue(holo.holo);
        assertTrue(holo.dark);
        assertNotEquals(material.background, holo.background);
        assertNotNull(Robolectric.buildActivity(SettingsActivity.class).create().get());
    }
    @Test public void snapshotKeepsStateAndDoesNotExposeCounters() {
        TunnelState.set("up", "");
        long[] counters = {42, 100, 3, 1};
        TunnelState.updateStats(counters);
        counters[0] = 0;
        assertEquals(42, TunnelState.stats()[0]);
        TunnelState.stats()[0] = 1;
        assertEquals(42, TunnelState.stats()[0]);
        assertTrue(TunnelState.active());
        TunnelState.set("failed", "failure");
        assertFalse(TunnelState.active());
        assertNull(TunnelState.stats());
        assertEquals("failure", TunnelState.error());
        TunnelState.set("off", "");
    }
}
