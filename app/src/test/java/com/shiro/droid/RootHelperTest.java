package com.shiro.droid;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RootHelperTest {
    @Test public void helperPathRejectsShellSyntax() throws Exception {
        assertEquals("/data/data/com.shiro.droid/files/rootd.sock", RootHelper.shellWord("/data/data/com.shiro.droid/files/rootd.sock"));
        for (String invalid : new String[] {"/data/a b", "/data/a;id", "/data/$(id)", "/data/`id`", "/data/a\n"}) {
            try { RootHelper.shellWord(invalid); fail("accepted " + invalid); } catch (IOException expected) { }
        }
    }
}
