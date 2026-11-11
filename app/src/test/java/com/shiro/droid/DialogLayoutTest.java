package com.shiro.droid;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
@Config(sdk = {19, 28})
public final class DialogLayoutTest {
    @Test public void stickerAndControlsFitSmallAndResizedWindows() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        ShiroDialog dialog = new ShiroDialog(activity, "A long dialog title that can wrap", R.drawable.sticker_ask);
        dialog.message("A long message for a narrow screen, which must remain scrollable when there is little room.");
        EditText field = new EditText(activity);
        field.setMinLines(3);
        dialog.field(field);
        dialog.cancelButton();
        dialog.button("Save", new View.OnClickListener() { @Override public void onClick(View v) {} });
        dialog.show();
        ViewGroup content = (ViewGroup)dialog.getWindow().findViewById(android.R.id.content);
        ViewGroup root = (ViewGroup)content.getChildAt(0);
        ImageView sticker = findImage(root);
        TextView heading = findText(root, "A long dialog title that can wrap");
        TextView save = findText(root, "Save");
        assertNotNull(sticker);
        assertNotNull(heading);
        assertNotNull(save);
        float density = activity.getResources().getDisplayMetrics().density;
        for (int[] size : new int[][] {{320, 480}, {320, 240}, {320, 200}, {640, 320}}) {
            int width = (int)(size[0] * density), height = (int)(size[1] * density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);
            Rect imageRect = bounds(root, sticker), textRect = bounds(root, heading), buttonRect = bounds(root, save);
            assertTrue("sticker overlaps title at " + size[1], imageRect.bottom + 8 * density <= textRect.top);
            assertTrue("button outside window at " + size[1], buttonRect.bottom <= height);
            assertTrue("button outside window at " + size[1], buttonRect.top >= 0);
        }
        dialog.dismiss();
    }
    @Test public void toolbarTitleIsVerticallyCenteredWithBackButton() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        ViewGroup page = (ViewGroup)ScreenChrome.wrap(activity, new Appearance(activity), "Settings", new View(activity));
        android.widget.LinearLayout bar = (android.widget.LinearLayout)page.getChildAt(0);
        TextView back = (TextView)bar.getChildAt(0);
        TextView title = (TextView)bar.getChildAt(1);
        float density = activity.getResources().getDisplayMetrics().density;
        page.measure(View.MeasureSpec.makeMeasureSpec((int)(320 * density), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((int)(480 * density), View.MeasureSpec.EXACTLY));
        page.layout(0, 0, page.getMeasuredWidth(), page.getMeasuredHeight());
        assertFalse(bar.isBaselineAligned());
        assertEquals(back.getTop(), title.getTop());
        assertEquals(back.getBottom(), title.getBottom());
        assertEquals(android.view.Gravity.CENTER_VERTICAL, title.getGravity() & android.view.Gravity.VERTICAL_GRAVITY_MASK);
    }
    private static Rect bounds(ViewGroup root, View child) {
        Rect result = new Rect();
        child.getDrawingRect(result);
        root.offsetDescendantRectToMyCoords(child, result);
        return result;
    }
    private static ImageView findImage(View view) {
        if (view instanceof ImageView) return (ImageView)view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ImageView found = findImage(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
    private static TextView findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView)view).getText())) return (TextView)view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
