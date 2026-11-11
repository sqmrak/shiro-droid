package com.shiro.droid;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

@android.annotation.TargetApi(14)
final class VpnConsent {
    static Intent prepare(Context context) { return VpnService.prepare(context); }
}
