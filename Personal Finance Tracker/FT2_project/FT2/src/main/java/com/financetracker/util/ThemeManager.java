package com.financetracker.util;

import com.financetracker.model.AppState;
import javafx.scene.Scene;

/**
 * Swaps CSS theme by toggling a style class on the root scene.
 * Light theme is the default (no extra class needed).
 * Dark theme adds the "dark" class which overrides CSS variables.
 */
public class ThemeManager {

    private static Scene scene;

    public static void setScene(Scene s) {
        scene = s;
    }

    public static void apply() {
        if (scene == null) return;
        if (AppState.get().isDarkMode()) {
            if (!scene.getRoot().getStyleClass().contains("dark")) {
                scene.getRoot().getStyleClass().add("dark");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark");
        }
    }

    public static void toggle() {
        AppState.get().setDarkMode(!AppState.get().isDarkMode());
        apply();
    }
}
