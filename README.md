# shiro-droid

<p align="center">
  <img src="assets/shiro-phone.png" width="420" alt="shiro-droid">
</p>

<p align="center"><b>cloudflare warp for the whole device, android 2.3 and later</b></p>

<p align="center">android port of <a href="https://github.com/sqmrak/shiro">shiro</a>, with its own app and build</p>

> [!NOTE]
> android 2.3 to 3.x needs root. android 4.0 and later uses `VpnService` without root.

## devices

- android 2.3 and later: `armeabi`, `armeabi-v7a`, `x86`
- android 5.0 and later: `arm64-v8a`, `x86_64`
- the quick settings tile needs android 7.0 or later
- `armeabi` is built for armv5te and runs on armv6 phones

## features

- home screen with connection state, errors and traffic counters
- settings for startup, reconnect, endpoint failover, dns, networks and the registration proxy
- material you and holo appearances, light and dark palettes
- russian, english and chinese, following the device language

## tunnel

the service registers an account, opens a `VpnService` tun and runs the wireguard packet core. it protects the warp udp socket from the vpn and tries up to five endpoints. direct and blocked ipv4 networks and tunnel dns come from private preferences.

ipv6 is routed to the tun and discarded by the ipv4-only core. the system must accept both the address and the route or connection fails. this still needs device verification.

the quick settings tile connects or disconnects the tunnel. if vpn permission is needed, the tile opens the app.

## registration

the first registration runs before the app asks for vpn permission, so an existing vpn can carry it. android replaces that vpn when shiro is granted permission and connects.

registration checks hostname and certificate against the bundled roots and can use a socks5 or http connect proxy. it starts with a tls client hello split inside the sni, based on the byebyedpi strategies. after a direct handshake timeout or network failure it tries a second split at the midpoint. each attempt has a 15 second timeout, and the proxy path makes one split attempt. this cannot bypass dns or ip blocking and does not carry the warp udp tunnel.

account bytes stay in private app storage; android backup is disabled.

## android 2.3 to 3.x

`shiro_rootd` opens the tun device, installs ipv4 routes, redirects dns and blocks ipv6 outside loopback. it needs `su`, a tun driver, `iptables` with nat and `ip6tables`. a watchdog removes the helper's firewall chains when the helper exits unexpectedly. connect at startup needs the app to be opened first.

the helper, service and watchdog still need validation on a real device.

## preferences

private `shiro` preferences. disconnect before changing them.

- `direct`, `block`: cidr lists. the dns address must not match either
- `dns`: default `1.1.1.1`
- `registration_proxy`: empty by default
- `auto_reconnect`, `failover`: on by default
- `connect_at_startup`: off by default

## status

no real android device, cloudflare registration, network transition, root firewall setup or reboot has been verified. the test apk is not a release.

## build

```bash
make test
make test-java
make test-android
make apk
```

`make test` runs host c tests, including a local wireguard responder and registration fixtures. `make test-java` exercises jni against the host library. `make test-android` runs service and activity smoke tests with robolectric. none connects a device or contacts cloudflare.

`make apk` writes `app/build/outputs/apk/debug/app-debug.apk`. `ABIS` selects architectures, default `armeabi armeabi-v7a arm64-v8a x86 x86_64`; 32 bit slices use api 9 and 64 bit slices api 21. `SHIRODROID_API` selects the 32 bit api.

needs jdk 17, the android sdk and gradle (`SHIRODROID_ANDROID`), ndk llvm and sysroot (`SHIRODROID_LLVM`, `SHIRODROID_SYSROOT`), ndk r15c (`SHIRODROID_NDK15`) and the openssl source (`SHIRODROID_OPENSSL_SRC`). `SHIRODROID_AAPT2` overrides aapt2. `SHIRODROID_ROBOLECTRIC` selects the framework jar cache for `make test-java`.

## license

[gpl v3](LICENSE). the core, tests and trust roots originate in `ios/shiro`. the armv5 atomic compatibility code and build scripts include code under [gpl v2](LICENSE.compat).
