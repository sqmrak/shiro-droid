package com.shiro.droid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MotionTest {
    @Test public void springSettlesAndColorBlendKeepsEndpoints() {
        assertEquals(0f, Motion.SPRING.getInterpolation(0f), 0f);
        assertEquals(1f, Motion.SPRING.getInterpolation(1f), 0f);
        assertTrue(Motion.SPRING.getInterpolation(.3f) > 1f);
        assertEquals(0xff102030, Tween.mix(0xff102030, 0xffa0b0c0, 0f));
        assertEquals(0xffa0b0c0, Tween.mix(0xff102030, 0xffa0b0c0, 1f));
    }
}
