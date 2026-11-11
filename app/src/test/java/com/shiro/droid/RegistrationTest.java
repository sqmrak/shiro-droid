package com.shiro.droid;

import android.content.Context;
import android.os.Looper;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = {19, 28})
public final class RegistrationTest {
    @Test public void successfulRegistrationFinishesBeforeVpnStart() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        Thread worker = Registration.begin(context, new Registration.Work() {
            @Override public void run() {}
        });
        assertEquals("registering", TunnelState.state());
        assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        worker.join(5000);
        assertFalse(worker.isAlive());
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(Registration.running());
        assertTrue(Registration.takeReady());
        assertFalse(Registration.takeReady());
        assertEquals("off", TunnelState.state());
    }
    @Test public void failedRegistrationDoesNotProceed() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        Thread worker = Registration.begin(context, new Registration.Work() {
            @Override public void run() throws IOException { throw new IOException("registration: timeout"); }
        });
        worker.join(5000);
        assertFalse(worker.isAlive());
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(Registration.takeReady());
        assertEquals("failed", TunnelState.state());
        assertEquals("registration: timeout", TunnelState.error());
        TunnelState.set("off", "");
    }
    @Test public void cancelledRegistrationCannotStartVpnLater() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        Thread worker = Registration.begin(context, new Registration.Work() {
            @Override public void run() throws IOException {
                entered.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test timed out"); }
                catch (InterruptedException e) { throw new IOException(e); }
            }
        });
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Registration.cancel(context);
        } finally { release.countDown(); }
        worker.join(5000);
        assertFalse(worker.isAlive());
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(Registration.running());
        assertFalse(Registration.takeReady());
        assertEquals("off", TunnelState.state());
        assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
}
