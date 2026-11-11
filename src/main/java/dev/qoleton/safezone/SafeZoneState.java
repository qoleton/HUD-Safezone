package dev.qoleton.safezone;

import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks whether we are currently "inside" the safe zone. While inside, {@code Window#getGuiScaledWidth/Height}
 * report the zone size (see WindowMixin). A stack lets full-screen layers (crosshair, vignette...) temporarily
 * step back out to the real screen.
 */
public final class SafeZoneState {
    public record Rect(int x, int y, int w, int h) {
    }

    private static final Deque<Rect> STACK = new ArrayDeque<>();
    /** Sentinel meaning "real screen" while nested inside a zone. */
    private static final Rect REAL = new Rect(0, 0, -1, -1);

    private SafeZoneState() {
    }

    /** Zone for a GUI of the given (real) scaled size. */
    public static Rect compute(int screenW, int screenH, double horizontal, double vertical) {
        int x = (int) Math.round(screenW * horizontal);
        int y = (int) Math.round(screenH * vertical);
        int w = Math.max(16, screenW - 2 * x);
        int h = Math.max(16, screenH - 2 * y);
        return new Rect(x, y, w, h);
    }

    /** Zone for the current window, using the saved config. Must be called while NOT entered (reads real size). */
    public static Rect computeCurrent() {
        var window = Minecraft.getInstance().getWindow();
        boolean wasEntered = isEntered();
        if (wasEntered) {
            STACK.push(REAL);
        }
        try {
            SafeZoneConfig c = SafeZoneConfig.get();
            return compute(window.getGuiScaledWidth(), window.getGuiScaledHeight(), c.horizontal, c.vertical);
        } finally {
            if (wasEntered) {
                STACK.pop();
            }
        }
    }

    /** Real GUI size {width, height}, even while entered. */
    public static int[] realSize() {
        var window = Minecraft.getInstance().getWindow();
        STACK.push(REAL);
        try {
            return new int[]{window.getGuiScaledWidth(), window.getGuiScaledHeight()};
        } finally {
            STACK.pop();
        }
    }

    public static boolean isEntered() {
        Rect top = STACK.peek();
        return top != null && top != REAL;
    }

    /** How far the current drawing is shifted from the real screen (0 when not entered). */
    public static int offsetX() {
        return isEntered() ? STACK.peek().x() : 0;
    }

    public static int offsetY() {
        return isEntered() ? STACK.peek().y() : 0;
    }

    public static Rect current() {
        return STACK.peek();
    }

    public static void enter(Rect r) {
        STACK.push(r);
    }

    /** Temporarily report the real screen size again (e.g. for the crosshair). */
    public static void enterReal() {
        STACK.push(REAL);
    }

    public static void exit() {
        if (!STACK.isEmpty()) {
            STACK.pop();
        }
    }
}
