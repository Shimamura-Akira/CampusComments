package com.example.campuscomments.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.campuscomments.R;

public final class WindowInsetUtils {
    private WindowInsetUtils() {
    }

    public static void applySystemBars(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }
        View root = content.getChildAt(0);
        int baseLeft = root.getPaddingLeft();
        int baseTop = root.getPaddingTop();
        int baseRight = root.getPaddingRight();
        int baseBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    baseLeft + bars.left,
                    baseTop + bars.top,
                    baseRight + bars.right,
                    baseBottom + bars.bottom);
            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    public static void bindBackButton(Activity activity) {
        View backButton = activity.findViewById(R.id.backButton);
        if (backButton == null) {
            return;
        }
        backButton.setOnClickListener(view -> {
            if (activity instanceof OnBackPressedDispatcherOwner) {
                ((OnBackPressedDispatcherOwner) activity).getOnBackPressedDispatcher().onBackPressed();
            } else {
                activity.onBackPressed();
            }
        });
    }
}
