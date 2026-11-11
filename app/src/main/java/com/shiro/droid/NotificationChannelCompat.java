package com.shiro.droid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

@android.annotation.TargetApi(26)
final class NotificationChannelCompat {
    private static final String CHANNEL = "shiro_vpn";
    static void create(Context context) {
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Shiro VPN", NotificationManager.IMPORTANCE_LOW));
    }
    static void setChannel(Notification.Builder builder) { builder.setChannelId(CHANNEL); }
}
