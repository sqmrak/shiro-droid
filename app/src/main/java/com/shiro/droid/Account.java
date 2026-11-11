package com.shiro.droid;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

final class Account {
    private static final int SIZE = 113;
    private Account() {}

    static boolean exists(Context context) {
        return new File(context.getFilesDir(), "shiro.account").exists();
    }

    static synchronized byte[] loadOrRegister(Context context, String proxy, Runnable onRegistration) throws IOException {
        File account = new File(context.getFilesDir(), "shiro.account");
        if (account.exists()) {
            byte[] bytes = read(new FileInputStream(account), SIZE);
            if (bytes.length != SIZE) throw new IOException("the saved WARP account is damaged");
            Native.address(bytes);
            return bytes;
        }
        if (onRegistration != null) onRegistration.run();
        byte[] roots = read(context.getAssets().open("roots.pem"), 2 * 1024 * 1024);
        byte[] bytes = Native.register(roots, proxy);
        File temp = new File(context.getFilesDir(), "shiro.account.tmp");
        boolean saved = false;
        try {
            FileOutputStream out = context.openFileOutput(temp.getName(), Context.MODE_PRIVATE);
            try {
                out.write(bytes);
                out.getFD().sync();
            } finally {
                out.close();
            }
            if (!temp.renameTo(account)) throw new IOException("cannot save the WARP account");
            saved = true;
            return bytes;
        } finally {
            if (!saved) {
                Arrays.fill(bytes, (byte)0);
                if (temp.exists() && !temp.delete()) android.util.Log.w("shiro", "cannot remove temporary account");
            }
        }
    }

    private static byte[] read(InputStream input, int max) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = input.read(buf)) != -1) {
                if (out.size() > max - n) throw new IOException("file is too large");
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            input.close();
        }
    }
}
