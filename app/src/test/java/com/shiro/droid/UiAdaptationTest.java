package com.shiro.droid;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = {19, 28})
public final class UiAdaptationTest {
    @Test public void onlyRegistrationTimeoutGetsVpnSuggestion() {
        Context context = RuntimeEnvironment.getApplication();
        assertEquals(context.getString(R.string.registration_timeout), MainActivity.displayError(context, "registration: timeout"));
        assertEquals(context.getString(R.string.error_unexpected_error) + ": registration: io", MainActivity.displayError(context, "registration: io"));
        assertEquals(context.getString(R.string.error_status_timeout), MainActivity.displayError(context, "timeout"));
        assertEquals("", MainActivity.displayError(context, ""));
    }
    @Test public void columnCapsContentAndKeepsNarrowScreensFullWidth() {
        Context context = RuntimeEnvironment.getApplication();
        ContentColumn column = new ContentColumn(context);
        View child = new View(context);
        column.addView(child);
        int cap = (int)(640 * context.getResources().getDisplayMetrics().density + .5f);
        column.measure(View.MeasureSpec.makeMeasureSpec(cap * 2, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        assertEquals(cap, child.getMeasuredWidth());
        column.measure(View.MeasureSpec.makeMeasureSpec(cap / 2, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        assertEquals(cap / 2, child.getMeasuredWidth());
    }
    @Test public void homeStacksInPortraitAndUsesTwoColumnsInLandscape() {
        Context context = RuntimeEnvironment.getApplication();
        float density = context.getResources().getDisplayMetrics().density;
        HomeLayout page = new HomeLayout(context);
        page.header = new View(context);
        page.header.setMinimumHeight((int)(64 * density));
        page.power = new View(context);
        page.info = new View(context);
        page.info.setMinimumHeight((int)(140 * density));
        page.addView(page.header); page.addView(page.power); page.addView(page.info);
        int width = (int)(360 * density), height = (int)(720 * density);
        page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        page.layout(0, 0, width, height);
        assertTrue(page.power.getBottom() <= page.info.getTop());
        assertTrue(page.power.getWidth() > 0);
        width = (int)(720 * density); height = (int)(360 * density);
        page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        page.layout(0, 0, width, height);
        assertTrue(page.power.getRight() <= page.info.getLeft());
        assertTrue(page.info.getBottom() <= height);
    }
    @Test public void homeButtonUsesAvailableSpaceWithoutCoveringOtherContent() {
        Context context = RuntimeEnvironment.getApplication();
        float density = context.getResources().getDisplayMetrics().density;
        HomeLayout page = new HomeLayout(context);
        page.header = new View(context); page.header.setMinimumHeight((int)(64 * density));
        page.power = new View(context);
        page.info = new View(context); page.info.setMinimumHeight((int)(140 * density));
        page.addView(page.header); page.addView(page.power); page.addView(page.info);
        int width = (int)(400 * density), height = (int)(850 * density);
        page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        page.layout(0, 0, width, height);
        assertTrue(page.power.getWidth() >= 320 * density);
        assertTrue(page.power.getBottom() <= page.info.getTop());
        assertTrue(page.header.getBottom() <= page.power.getTop());
    }
    @Test public void tabletAndTvFitTwoColumnsAndPortraitStacks() {
        Context context = RuntimeEnvironment.getApplication();
        float density = context.getResources().getDisplayMetrics().density;
        int[][] sizes = {{1280, 720}, {1024, 768}, {800, 1280}};
        for (int[] size : sizes) {
            HomeLayout page = new HomeLayout(context);
            page.header = new View(context); page.header.setMinimumHeight((int)(64 * density));
            page.power = new View(context);
            page.info = new View(context); page.info.setMinimumHeight((int)(160 * density));
            page.addView(page.header); page.addView(page.power); page.addView(page.info);
            int width = (int)(size[0] * density), height = (int)(size[1] * density);
            page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            page.layout(0, 0, width, height);
            assertTrue(page.power.getWidth() > 0);
            assertTrue(page.power.getBottom() <= height);
            assertTrue(page.info.getBottom() <= height);
            if (width > height) assertTrue(page.power.getRight() <= page.info.getLeft());
            else assertTrue(page.power.getBottom() <= page.info.getTop());
            assertEquals(page.header.getLeft(), width - page.header.getRight());
            assertTrue(page.header.getLeft() <= 28 * density);
            assertTrue(width - page.header.getRight() <= 28 * density);
        }
    }
    @Test public void settingsControlsAndSelectedChoiceShowDpadFocus() {
        SettingsActivity settings = org.robolectric.Robolectric.buildActivity(SettingsActivity.class).setup().get();
        TextView dns = findText(settings.getWindow().getDecorView(), settings.getString(R.string.dns));
        View row = (View)dns.getParent();
        assertTrue(row.isFocusable());
        assertTrue(row.getBackground().isStateful());
        TextView startup = findText(settings.getWindow().getDecorView(), settings.getString(R.string.startup));
        android.view.ViewGroup toggleRow = (android.view.ViewGroup)startup.getParent();
        View toggle = toggleRow.getChildAt(toggleRow.getChildCount() - 1);
        assertTrue(toggle.isFocusable());
        assertFalse(toggleRow.isFocusable());
        android.app.Activity activity = org.robolectric.Robolectric.buildActivity(android.app.Activity.class).create().get();
        ShiroDialog dialog = new ShiroDialog(activity, "title", R.drawable.sticker_ask);
        dialog.choice("selected", true, new Runnable() { public void run() {} });
        dialog.show();
        TextView choice = findText(dialog.getWindow().getDecorView(), "selected");
        assertTrue(choice.isFocusable());
        assertTrue(choice.getBackground().isStateful());
        dialog.dismiss();
    }
    @Test public void dpadCenterChangesSettingsSwitch() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("shiro", 0).edit().putBoolean("connect_at_startup", false).commit();
        SettingsActivity settings = org.robolectric.Robolectric.buildActivity(SettingsActivity.class).setup().get();
        TextView startup = findText(settings.getWindow().getDecorView(), settings.getString(R.string.startup));
        android.view.ViewGroup row = (android.view.ViewGroup)startup.getParent();
        View control = row.getChildAt(row.getChildCount() - 1);
        assertTrue(control.requestFocus());
        control.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_CENTER));
        control.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_CENTER));
        assertTrue(context.getSharedPreferences("shiro", 0).getBoolean("connect_at_startup", false));
    }
    @Test public void aboutShowsShiroCreditsAndOpensFromSettings() {
        SettingsActivity settings = org.robolectric.Robolectric.buildActivity(SettingsActivity.class).setup().get();
        TextView entry = findText(settings.getWindow().getDecorView(), settings.getString(R.string.about));
        assertNotNull(entry);
        ((View)entry.getParent()).performClick();
        android.content.Intent intent = org.robolectric.Shadows.shadowOf(settings).getNextStartedActivity();
        assertEquals(AboutActivity.class.getName(), intent.getComponent().getClassName());
        AboutActivity about = org.robolectric.Robolectric.buildActivity(AboutActivity.class).setup().get();
        assertNotNull(findText(about.getWindow().getDecorView(), "sqmrak"));
        assertNotNull(findText(about.getWindow().getDecorView(), "@QWERTYU1OPZ"));
        assertNotNull(findText(about.getWindow().getDecorView(), "GNU GPL v3"));
        TextView telegram = findText(about.getWindow().getDecorView(), "t.me/sqmrakdev");
        assertNotNull(telegram);
        View row = (View)telegram.getParent().getParent();
        assertTrue(row.isFocusable());
        row.performClick();
        android.content.Intent link = org.robolectric.Shadows.shadowOf(about).getNextStartedActivity();
        assertEquals("https://t.me/sqmrakdev", link.getDataString());
    }
    @Test public void homeAndAboutUseShiroIdentity() throws Exception {
        MainActivity main = org.robolectric.Robolectric.buildActivity(MainActivity.class).setup().get();
        assertNotNull(findText(main.getWindow().getDecorView(), "shiro-droid"));
        assertNotNull(findText(main.getWindow().getDecorView(), "by sqmrak :3"));
        assertEquals("1.0.0-canary", main.getPackageManager().getPackageInfo(main.getPackageName(), 0).versionName);
        AboutActivity about = org.robolectric.Robolectric.buildActivity(AboutActivity.class).setup().get();
        assertNotNull(findText(about.getWindow().getDecorView(), "1.0.0-canary"));
    }
    @Test public void dialogChoicesAndButtonsAcceptDpadFocus() {
        android.app.Activity activity = org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup().get();
        ShiroDialog dialog = new ShiroDialog(activity, "title", R.drawable.sticker_ask);
        final boolean[] chosen = {false};
        dialog.choice("choice", false, new Runnable() { public void run() { chosen[0] = true; }});
        dialog.cancelButton();
        dialog.show();
        TextView choice = findText(dialog.getWindow().getDecorView(), "choice");
        assertNotNull(choice);
        assertTrue(choice.isFocusable());
        choice.performClick();
        assertTrue(chosen[0]);
        assertFalse(dialog.isShowing());
    }
    @Test public void settingsSwitchPersistsAndRowsOpenEditors() {
        SettingsActivity settings = org.robolectric.Robolectric.buildActivity(SettingsActivity.class).setup().get();
        android.content.SharedPreferences prefs = settings.getSharedPreferences("shiro", 0);
        prefs.edit().putBoolean("connect_at_startup", false).commit();
        TextView startup = findText(settings.getWindow().getDecorView(), settings.getString(R.string.startup));
        assertNotNull(startup);
        View row = (View)startup.getParent();
        row.performClick();
        assertTrue(prefs.getBoolean("connect_at_startup", false));
        row.performClick();
        assertFalse(prefs.getBoolean("connect_at_startup", true));
        TextView dns = findText(settings.getWindow().getDecorView(), settings.getString(R.string.dns));
        assertNotNull(dns);
        ((View)dns.getParent()).performClick();
        assertNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog());
    }
    @Test public void themeChoiceMarksTheSavedStyle() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("shiro", 0).edit().putString("theme", "holo").commit();
        SettingsActivity settings = org.robolectric.Robolectric.buildActivity(SettingsActivity.class).setup().get();
        TextView theme = findText(settings.getWindow().getDecorView(), settings.getString(R.string.theme));
        ((View)theme.getParent()).performClick();
        android.app.Dialog dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        TextView holo = findText(dialog.getWindow().getDecorView(), "Holo");
        TextView material = findText(dialog.getWindow().getDecorView(), "Material You");
        assertNotNull(holo.getCompoundDrawables()[2]);
        assertNull(material.getCompoundDrawables()[2]);
        material.performClick();
        assertEquals("material", context.getSharedPreferences("shiro", 0).getString("theme", ""));
    }
    private TextView findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView)view).getText())) return (TextView)view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
