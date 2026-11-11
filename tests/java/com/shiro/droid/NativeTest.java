package com.shiro.droid;

import java.io.IOException;

public final class NativeTest {
    private interface Call { void run() throws IOException; }
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    private static void fails(Call call) throws IOException {
        try { call.run(); throw new AssertionError("expected IOException"); }
        catch (IOException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        byte[] data = new byte[113];
        data[0] = 'E'; data[1] = 'X'; data[2] = 'W'; data[3] = '1';
        data[72] = 2; data[73] = 0; data[74] = 16; data[75] = (byte)172;
        data[76] = 1; data[77] = 0; data[78] = 0; data[79] = 127;
        data[80] = 1; data[81] = 104; data[82] = 9;
        check("172.16.0.2".equals(Native.address(data)));
        check(Native.routes("", "").length == 1);
        check("0.0.0.0/0".equals(Native.routes("", "")[0]));
        check(Native.routes("10.0.0.0/8", "").length == 8);
        Native.validateDns("1.1.1.1", "10.0.0.0/8", "");
        fails(new Call() { public void run() throws IOException { Native.validateDns("10.0.0.1", "10.0.0.0/8", ""); } });
        fails(new Call() { public void run() throws IOException { Native.validateDns("1.1.1.1", "", "1.1.1.0/24"); } });
        fails(new Call() { public void run() throws IOException { Native.validateDns("example.com", "", ""); } });
        fails(new Call() { public void run() throws IOException { Native.routes("10.0.0.1/8", ""); } });
        fails(new Call() { public void run() throws IOException { Native.address(new byte[1]); } });
        fails(new Call() { public void run() throws IOException { Native.register(new byte[0], "bad-proxy"); } });
        fails(new Call() { public void run() throws IOException { Native.rootOpen("/no/such/socket", "172.16.0.2", "1.1.1.1", "", ""); } });
        check(!Native.rootProtect("/no/such/socket", -1));
        System.out.println("jni: " + checks + " checks passed");
    }
}
