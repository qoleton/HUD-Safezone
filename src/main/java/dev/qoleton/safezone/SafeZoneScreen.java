package dev.qoleton.safezone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Console-style safe zone editor. Drag the edges of the frame (or use the sliders / presets) until the
 * corner markers sit where you want the HUD to stop.
 */
public class SafeZoneScreen extends Screen {
    private static final int EDGE_GRAB = 6;
    private static final int ACCENT = 0xFFFFC94A;

    private final Screen parent;
    private final SafeZoneConfig cfg = SafeZoneConfig.get();
    private final double startH = cfg.horizontal, startV = cfg.vertical;
    private final boolean startHud = cfg.hud, startMenus = cfg.menus;
    private final SafeZoneConfig.MenuBackground startBackground = cfg.menuBackground;
    private Integer startBlur;

    private enum Drag { NONE, HORIZONTAL, VERTICAL }

    private Drag dragging = Drag.NONE;

    public SafeZoneScreen(Screen parent) {
        super(Component.literal("Safe Zone"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (startBlur == null) {
            startBlur = minecraft.options.menuBackgroundBlurriness().get();
        }
        int cx = width / 2;
        int y = height / 2 - 86;
        int full = 224, half = 110, third = 72;

        addRenderableWidget(Button.builder(Component.literal("Full screen"), b -> preset(0))
                .bounds(cx - full / 2, y, third, 20).tooltip(tip("No frame. Everything uses the whole screen.")).build());
        addRenderableWidget(Button.builder(Component.literal("21:9 (3440)"), b -> preset(3440.0 / 1440.0))
                .bounds(cx - third / 2, y, third, 20).tooltip(tip("Frame the size of a 3440x1440 ultrawide.")).build());
        addRenderableWidget(Button.builder(Component.literal("16:9 (2560)"), b -> preset(2560.0 / 1440.0))
                .bounds(cx + full / 2 - third, y, third, 20).tooltip(tip("Frame the size of a normal 16:9 screen.")).build());
        y += 24;

        MarginSlider widthSlider = new MarginSlider(cx - full / 2, y, full, 20, true);
        widthSlider.setTooltip(tip("How wide the frame is."));
        addRenderableWidget(widthSlider);
        y += 24;
        MarginSlider heightSlider = new MarginSlider(cx - full / 2, y, full, 20, false);
        heightSlider.setTooltip(tip("How tall the frame is."));
        addRenderableWidget(heightSlider);
        y += 24;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.hud)
                .withTooltip(v -> tip("Keep the in-game HUD (hotbar, chat, minimap...) inside the frame."))
                .create(cx - full / 2, y, half, 20, Component.literal("HUD"), (b, v) -> cfg.hud = v));
        addRenderableWidget(CycleButton.onOffBuilder(cfg.menus)
                .withTooltip(v -> tip("Keep menus (inventory, EMI/JEI, settings...) inside the frame."))
                .create(cx + full / 2 - half, y, half, 20, Component.literal("Menus"), (b, v) -> cfg.menus = v));
        y += 24;

        addRenderableWidget(CycleButton.<SafeZoneConfig.MenuBackground>builder(m -> Component.literal(m.label))
                .withValues(SafeZoneConfig.MenuBackground.values())
                .withInitialValue(cfg.menuBackground)
                .withTooltip(v -> tip("The dark background behind menus: whole screen, frame only, or none."))
                .create(cx - full / 2, y, full, 20, Component.literal("Menu background"), (b, v) -> cfg.menuBackground = v));
        y += 24;

        // Vanilla's "Menu Background Blurriness" option (Accessibility), surfaced here for convenience.
        var blur = minecraft.options.menuBackgroundBlurriness().createButton(minecraft.options, cx - full / 2, y, full);
        blur.setTooltip(tip("How blurry the game looks behind menus. 0 = no blur."));
        addRenderableWidget(blur);
        y += 24;

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancel())
                .bounds(cx - full / 2, y, third, 20).tooltip(tip("Close without saving changes.")).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> preset(0))
                .bounds(cx - third / 2, y, third, 20).tooltip(tip("Remove the frame.")).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx + full / 2 - third, y, third, 20).tooltip(tip("Save and close.")).build());
    }

    private static Tooltip tip(String text) {
        return Tooltip.create(Component.literal(text));
    }

    /** aspect 0 = no zone; otherwise fit a frame of that aspect ratio into the current window. */
    private void preset(double aspect) {
        cfg.horizontal = 0;
        cfg.vertical = 0;
        if (aspect > 0) {
            // Use real window pixels, not rounded GUI units, so the frame matches an OBS crop exactly.
            var window = minecraft.getWindow();
            double screenAspect = (double) window.getWidth() / window.getHeight();
            if (screenAspect > aspect) {
                cfg.horizontal = (1.0 - aspect / screenAspect) / 2.0;
            } else {
                cfg.vertical = (1.0 - screenAspect / aspect) / 2.0;
            }
        }
        cfg.clamp();
        rebuildWidgets();
    }

    private void cancel() {
        cfg.horizontal = startH;
        cfg.vertical = startV;
        cfg.hud = startHud;
        cfg.menus = startMenus;
        cfg.menuBackground = startBackground;
        minecraft.options.menuBackgroundBlurriness().set(startBlur);
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        SafeZoneConfig.save();
        minecraft.options.save();
        minecraft.setScreen(parent);
    }

    private SafeZoneState.Rect zone() {
        return SafeZoneState.compute(width, height, cfg.horizontal, cfg.vertical);
    }

    // ------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        SafeZoneState.Rect r = zone();
        int x0 = r.x(), y0 = r.y(), x1 = r.x() + r.w(), y1 = r.y() + r.h();

        // Shade everything outside the zone.
        int shade = 0xB0000000;
        g.fill(0, 0, width, y0, shade);
        g.fill(0, y1, width, height, shade);
        g.fill(0, y0, x0, y1, shade);
        g.fill(x1, y0, width, y1, shade);

        // Thin frame + highlighted edge when hovered/dragged.
        boolean hotH = dragging == Drag.HORIZONTAL || (dragging == Drag.NONE && nearVerticalEdge(mouseX, mouseY, r));
        boolean hotV = dragging == Drag.VERTICAL || (dragging == Drag.NONE && nearHorizontalEdge(mouseX, mouseY, r));
        int line = 0x80FFFFFF;
        g.fill(x0, y0, x1, y0 + 1, hotV ? ACCENT : line);
        g.fill(x0, y1 - 1, x1, y1, hotV ? ACCENT : line);
        g.fill(x0, y0, x0 + 1, y1, hotH ? ACCENT : line);
        g.fill(x1 - 1, y0, x1, y1, hotH ? ACCENT : line);

        // Console-style corner brackets.
        int len = 18, t = 3;
        corner(g, x0, y0, len, t, 1, 1);
        corner(g, x1, y0, len, t, -1, 1);
        corner(g, x0, y1, len, t, 1, -1);
        corner(g, x1, y1, len, t, -1, -1);

        // Mock HUD so you can see where things will land.
        mock(g, x1 - 70, y0 + 6, x1 - 6, y0 + 70, "Minimap");
        mock(g, x0 + 6, y0 + 6, x0 + 70, y0 + 22, "Effects");
        mock(g, x0 + 6, y1 - 64, x0 + 150, y1 - 26, "Chat");
        int hx = x0 + r.w() / 2;
        mock(g, hx - 91, y1 - 22, hx + 91, y1, "Hotbar");
        mock(g, x1 - 90, y1 - 120, x1 - 6, y1 - 30, "EMI / JEI");

        // Info text.
        int top = height / 2 - 124;
        g.drawCenteredString(font, "SAFE ZONE", width / 2, top, ACCENT);
        g.drawCenteredString(font, "Drag the frame edges or pick a preset. Hover a button for help.", width / 2, top + 12, 0xFFDDDDDD);
        double pxW = r.w() * minecraft.getWindow().getGuiScale();
        double pxH = r.h() * minecraft.getWindow().getGuiScale();
        var window = minecraft.getWindow();
        String info = String.format("Frame: %d x %d px  (%.2f:9)    Window: %d x %d px",
                Math.round(pxW), Math.round(pxH), 9.0 * pxW / pxH, window.getWidth(), window.getHeight());
        g.drawCenteredString(font, info, width / 2, top + 24, 0xFFAAAAAA);
    }

    private static void corner(GuiGraphics g, int x, int y, int len, int t, int dx, int dy) {
        int xa = Math.min(x, x + dx * len), xb = Math.max(x, x + dx * len);
        int ya = Math.min(y, y + dy * t), yb = Math.max(y, y + dy * t);
        g.fill(xa, ya, xb, yb, ACCENT);
        xa = Math.min(x, x + dx * t);
        xb = Math.max(x, x + dx * t);
        ya = Math.min(y, y + dy * len);
        yb = Math.max(y, y + dy * len);
        g.fill(xa, ya, xb, yb, ACCENT);
    }

    private void mock(GuiGraphics g, int x0, int y0, int x1, int y1, String label) {
        g.fill(x0, y0, x1, y1, 0x40FFFFFF);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0x80FFFFFF);
        g.drawCenteredString(font, label, (x0 + x1) / 2, (y0 + y1) / 2 - 4, 0xFFEEEEEE);
    }

    // --------------------------------------------------------- edge drag

    private static boolean nearVerticalEdge(double mx, double my, SafeZoneState.Rect r) {
        boolean inY = my >= r.y() && my <= r.y() + r.h();
        return inY && (Math.abs(mx - r.x()) <= EDGE_GRAB || Math.abs(mx - (r.x() + r.w())) <= EDGE_GRAB);
    }

    private static boolean nearHorizontalEdge(double mx, double my, SafeZoneState.Rect r) {
        boolean inX = mx >= r.x() && mx <= r.x() + r.w();
        return inX && (Math.abs(my - r.y()) <= EDGE_GRAB || Math.abs(my - (r.y() + r.h())) <= EDGE_GRAB);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button == 0) {
            SafeZoneState.Rect r = zone();
            if (nearVerticalEdge(mx, my, r)) {
                dragging = Drag.HORIZONTAL;
                return true;
            }
            if (nearHorizontalEdge(mx, my, r)) {
                dragging = Drag.VERTICAL;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == Drag.HORIZONTAL) {
            cfg.horizontal = Math.min(mx, width - mx) / width;
            cfg.clamp();
            syncSliders();
            return true;
        }
        if (dragging == Drag.VERTICAL) {
            cfg.vertical = Math.min(my, height - my) / height;
            cfg.clamp();
            syncSliders();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != Drag.NONE) {
            dragging = Drag.NONE;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private void syncSliders() {
        for (var child : children()) {
            if (child instanceof MarginSlider s) {
                s.sync();
            }
        }
    }

    // ------------------------------------------------------------ slider

    /** Shows the visible size in % (100% = no margin). */
    private class MarginSlider extends AbstractSliderButton {
        private final boolean horizontal;

        MarginSlider(int x, int y, int w, int h, boolean horizontal) {
            super(x, y, w, h, Component.empty(), 0);
            this.horizontal = horizontal;
            sync();
        }

        private double max() {
            return horizontal ? SafeZoneConfig.MAX_H : SafeZoneConfig.MAX_V;
        }

        void sync() {
            double margin = horizontal ? cfg.horizontal : cfg.vertical;
            this.value = 1.0 - margin / max();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double margin = (1.0 - value) * max();
            int pct = (int) Math.round((1.0 - 2 * margin) * 100);
            setMessage(Component.literal((horizontal ? "Width: " : "Height: ") + pct + "%"));
        }

        @Override
        protected void applyValue() {
            double margin = (1.0 - value) * max();
            if (horizontal) {
                cfg.horizontal = margin;
            } else {
                cfg.vertical = margin;
            }
            cfg.clamp();
        }
    }
}
