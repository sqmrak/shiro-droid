package com.shiro.droid;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = {19, 28}, qualifiers = "ru")
public final class ErrorTextTest {
    @Test public void vpnAndRegistrationErrorsAreLocalized() {
        Context c = RuntimeEnvironment.getApplication();
        assertEquals("Разрешение VPN было отозвано", ErrorText.format(c, "VPN permission was revoked"));
        assertEquals("Разрешение VPN отклонено или отозвано", ErrorText.format(c, "VPN permission was refused or revoked"));
        assertEquals("Разрешение VPN отклонено", ErrorText.format(c, "VPN permission was refused"));
        assertEquals("Попробуйте ещё раз с внешним VPN", ErrorText.format(c, "registration: timeout"));
        assertEquals("Регистрация: ошибка сети или ввода-вывода", ErrorText.format(c, "registration: io error"));
        assertEquals("Регистрация: сервер вернул ошибку (HTTP 403)", ErrorText.format(c, "registration: rpc error (HTTP 403)"));
    }
    @Test public void settingsAndOtherErrorsKeepTheirMeaning() {
        Context c = RuntimeEnvironment.getApplication();
        assertEquals("DNS должен быть IPv4-адресом, проходящим через WARP", ErrorText.format(c, "DNS must be an IPv4 address routed through WARP: bad argument"));
        assertEquals("Правила маршрутизации: некорректные данные", ErrorText.format(c, "routing rules: malformed input"));
        assertEquals("время ожидания истекло", ErrorText.format(c, "timeout"));
        assertEquals("Не удалось открыть /dev/tun: Permission denied", ErrorText.format(c, "cannot open /dev/tun: Permission denied"));
        assertEquals("Нет сети с маршрутом по умолчанию", ErrorText.format(c, "no network has a default route"));
        assertEquals("Ошибка: unexpected diagnostic", ErrorText.format(c, "unexpected diagnostic"));
        assertEquals("", ErrorText.format(c, null));
    }
}
