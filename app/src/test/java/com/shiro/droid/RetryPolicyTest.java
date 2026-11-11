package com.shiro.droid;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class RetryPolicyTest {
    @Test public void retriesAreBoundedAndAnEstablishedConnectionRestartsFromTheFirstServer() {
        assertEquals(-1, RetryPolicy.next(0, true, false, true));
        assertEquals(0, RetryPolicy.next(3, true, true, true));
        assertEquals(1, RetryPolicy.next(0, false, true, true));
        assertEquals(4, RetryPolicy.next(3, false, true, true));
        assertEquals(-1, RetryPolicy.next(4, false, true, true));
        assertEquals(0, RetryPolicy.next(0, true, true, false));
        assertEquals(-1, RetryPolicy.next(0, false, true, false));
    }
}
