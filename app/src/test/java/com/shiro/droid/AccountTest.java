package com.shiro.droid;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = {19, 28})
public final class AccountTest {
    @Test public void savedDamagedAccountDoesNotReportRegistration() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File account = new File(context.getFilesDir(), "shiro.account");
        FileOutputStream out = new FileOutputStream(account);
        out.write(1);
        out.close();
        final boolean[] registered = {false};
        try {
            Account.loadOrRegister(context, "", new Runnable() {
                @Override public void run() { registered[0] = true; }
            });
            fail("damaged account accepted");
        } catch (IOException expected) {
            assertFalse(registered[0]);
        } finally {
            assertTrue(account.delete());
        }
    }
}
