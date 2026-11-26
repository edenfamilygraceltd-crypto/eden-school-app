package com.grace.eden_smart;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps form fields visible when the Android keyboard is open.
 * Handles edge-to-edge window insets on recent Android versions.
 */
public final class KeyboardInsets {
    private KeyboardInsets() { }

    public static void apply(Activity activity) {
        if (activity == null) return;
        Window window = activity.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowCompat.setDecorFitsSystemWindows(window, false);

        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        final boolean[] keyboardWasVisible = { false };

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

            view.setPadding(left, top + bars.top, right,
                    bottom + Math.max(bars.bottom, ime.bottom));

            if (keyboardVisible && !keyboardWasVisible[0]) {
                view.post(() -> {
                    View focused = activity.getCurrentFocus();
                    if (focused != null) {
                        Rect visibleRect = new Rect(0, 0, focused.getWidth(), focused.getHeight());
                        focused.requestRectangleOnScreen(visibleRect, true);
                    }
                });
            }
            keyboardWasVisible[0] = keyboardVisible;
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
