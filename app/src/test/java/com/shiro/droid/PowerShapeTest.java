package com.shiro.droid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PowerShapeTest {
    @Test public void bothCyclesMorphAndClose() {
        assertTrue(PowerShape.working("registering"));
        assertTrue(PowerShape.lit("connecting"));
        assertTrue(PowerShape.lit("up"));
        assertFalse(PowerShape.lit("failed"));
        for (int i = 0; i < PowerShape.POINTS; i++) {
            assertEquals(PowerShape.radius(i, "connecting", 0), PowerShape.radius(i, "connecting", 2.8f), .0001f);
            assertEquals(PowerShape.radius(i, "up", 0), PowerShape.radius(i, "up", 6.4f), .0001f);
            for (int step = 0; step <= 32; step++) {
                float radius = PowerShape.radius(i, "connecting", step / 4f);
                assertTrue(radius >= .85f && radius <= 1.15f);
            }
        }
        assertNotEquals(PowerShape.radius(9, "connecting", 0), PowerShape.radius(9, "connecting", .7f), .001f);
        assertNotEquals(PowerShape.radius(9, "up", 0), PowerShape.radius(9, "up", 1.6f), .001f);
    }
    @Test public void rotationKeepsPositionAndSpeedAtTransitions() {
        float elapsed = .3f;
        float angle = PowerShape.angle(23f, elapsed, 72f, 30f);
        float speed = PowerShape.speed(elapsed, 72f, 30f);
        assertEquals(angle, PowerShape.angle(angle, 0, speed, 0), .0001f);
        assertEquals(speed, PowerShape.speed(0, speed, 0), .0001f);
        assertEquals(30f, PowerShape.speed(PowerShape.TURN_SECONDS, 72f, 30f), .0001f);
        float before = PowerShape.angle(0, PowerShape.TURN_SECONDS - .001f, 72f, 30f);
        float after = PowerShape.angle(0, PowerShape.TURN_SECONDS + .001f, 72f, 30f);
        assertEquals(.06f, after - before, .001f);
    }
}
