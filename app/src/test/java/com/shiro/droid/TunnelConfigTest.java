package com.shiro.droid;

import android.net.VpnService;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = 28, shadows = TunnelConfigTest.NumericBuilder.class)
public final class TunnelConfigTest {
    @Implements(VpnService.Builder.class)
    public static class NumericBuilder {
        @RealObject VpnService.Builder builder;
        static final List<String> routes = new ArrayList<String>();
        @Implementation protected VpnService.Builder addAddress(String address, int prefix) throws UnknownHostException {
            return builder.addAddress(InetAddress.getByName(address), prefix);
        }
        @Implementation protected VpnService.Builder addRoute(String address, int prefix) throws UnknownHostException {
            routes.add(address + "/" + prefix);
            return builder;
        }
        @Implementation protected VpnService.Builder addDnsServer(String address) throws UnknownHostException {
            return builder.addDnsServer(InetAddress.getByName(address));
        }
    }

    @Test public void ipv4TunnelDoesNotAdvertiseAnIpv6Path() throws Exception {
        NumericBuilder.routes.clear();
        ShiroVpnService service = Robolectric.buildService(ShiroVpnService.class).create().get();
        VpnService.Builder builder = service.new Builder();
        ShiroVpnService.configureTunnel(builder, "172.16.0.2", new String[]{"0.0.0.0/0"}, "1.1.1.1");
        Field configField = builder.getClass().getDeclaredField("mConfig");
        configField.setAccessible(true);
        Object config = configField.get(builder);
        assertTrue(config.getClass().getField("allowIPv4").getBoolean(config));
        assertFalse(config.getClass().getField("allowIPv6").getBoolean(config));
        assertEquals(1, NumericBuilder.routes.size());
        assertEquals("0.0.0.0/0", NumericBuilder.routes.get(0));
        assertEquals("1.1.1.1", ((List<?>)config.getClass().getField("dnsServers").get(config)).get(0));
        service.onDestroy();
    }
    @Test @Config(sdk = 19, shadows = NumericBuilder.class)
    public void oldAndroidStillRoutesIpv6IntoTheTunnel() throws Exception {
        NumericBuilder.routes.clear();
        ShiroVpnService service = Robolectric.buildService(ShiroVpnService.class).create().get();
        VpnService.Builder builder = service.new Builder();
        ShiroVpnService.configureTunnel(builder, "172.16.0.2", new String[]{"0.0.0.0/0"}, "1.1.1.1");
        Field addressesField = builder.getClass().getDeclaredField("mAddresses");
        addressesField.setAccessible(true);
        assertEquals(2, ((List<?>)addressesField.get(builder)).size());
        assertTrue(NumericBuilder.routes.contains("::/0"));
        assertTrue(NumericBuilder.routes.contains("0.0.0.0/0"));
        service.onDestroy();
    }

}
