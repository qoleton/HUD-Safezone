package dev.qoleton.safezone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Set;

public final class SafeZoneEvents {
    private SafeZoneEvents() {
    }

    /** HUD layers that must keep covering the whole screen / stay dead-centre. */
    private static final Set<ResourceLocation> FULL_SCREEN_LAYERS = Set.of(
            VanillaGuiLayers.CAMERA_OVERLAYS,
            VanillaGuiLayers.CROSSHAIR
    );

    /** Set by GameRendererMixin while the whole HUD draw is wrapped in the safe zone. */
    public static boolean hudEntered;
    private static int fullScreenLayerDepth;
    private static boolean redispatching;

    // ------------------------------------------------------------------ HUD

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!hudEntered || !FULL_SCREEN_LAYERS.contains(event.getName())) {
            return;
        }
        SafeZoneState.Rect r = SafeZoneState.current();
        GuiGraphics g = event.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(-r.x(), -r.y(), 0);
        SafeZoneState.enterReal();
        fullScreenLayerDepth++;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        if (fullScreenLayerDepth == 0 || !FULL_SCREEN_LAYERS.contains(event.getName())) {
            return;
        }
        fullScreenLayerDepth--;
        SafeZoneState.exit();
        event.getGuiGraphics().pose().popPose();
    }

    // ---------------------------------------------------------------- menus

    public static boolean constrains(Screen screen) {
        return screen != null
                && SafeZoneConfig.get().menusActive()
                && !(screen instanceof SafeZoneScreen)
                && !(screen instanceof TitleScreen);
    }

    /** Lay the menu out as if the window were only as big as the zone. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInitPre(ScreenEvent.Init.Pre event) {
        Screen s = event.getScreen();
        if (constrains(s)) {
            SafeZoneState.Rect r = SafeZoneState.computeCurrent();
            s.width = r.w();
            s.height = r.h();
        } else {
            // Opened from inside a constrained menu (e.g. the Safe Zone button on Options): the window
            // reported the zone size, but this screen should use the whole screen.
            int[] real = SafeZoneState.realSize();
            s.width = real[0];
            s.height = real[1];
        }
    }

    /** Adds the "Safe Zone..." button to the Options screen. */
    @SubscribeEvent
    public static void onInitPost(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof OptionsScreen options) {
            event.addListener(Button.builder(Component.literal("Safe Zone..."),
                            b -> options.getMinecraft().setScreen(new SafeZoneScreen(options)))
                    .bounds(6, 6, 90, 20)
                    .build());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen s = event.getScreen();
        if (redispatching || !constrains(s)) {
            return;
        }
        event.setCanceled(true);
        SafeZoneState.Rect r = begin();
        try {
            double x = event.getMouseX() - r.x(), y = event.getMouseY() - r.y();
            int button = event.getButton();
            if (!ClientHooks.onScreenMouseClickedPre(s, x, y, button)) {
                boolean handled = s.mouseClicked(x, y, button);
                ClientHooks.onScreenMouseClickedPost(s, x, y, button, handled);
            }
        } finally {
            end();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        Screen s = event.getScreen();
        if (redispatching || !constrains(s)) {
            return;
        }
        event.setCanceled(true);
        SafeZoneState.Rect r = begin();
        try {
            double x = event.getMouseX() - r.x(), y = event.getMouseY() - r.y();
            int button = event.getButton();
            if (!ClientHooks.onScreenMouseReleasedPre(s, x, y, button)) {
                boolean handled = s.mouseReleased(x, y, button);
                ClientHooks.onScreenMouseReleasedPost(s, x, y, button, handled);
            }
        } finally {
            end();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        Screen s = event.getScreen();
        if (redispatching || !constrains(s)) {
            return;
        }
        event.setCanceled(true);
        SafeZoneState.Rect r = begin();
        try {
            double x = event.getMouseX() - r.x(), y = event.getMouseY() - r.y();
            int button = event.getMouseButton();
            double dx = event.getDragX(), dy = event.getDragY();
            if (!ClientHooks.onScreenMouseDragPre(s, x, y, button, dx, dy)) {
                s.mouseDragged(x, y, button, dx, dy);
                ClientHooks.onScreenMouseDragPost(s, x, y, button, dx, dy);
            }
        } finally {
            end();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Screen s = event.getScreen();
        if (redispatching || !constrains(s)) {
            return;
        }
        event.setCanceled(true);
        SafeZoneState.Rect r = begin();
        try {
            double x = event.getMouseX() - r.x(), y = event.getMouseY() - r.y();
            double dx = event.getScrollDeltaX(), dy = event.getScrollDeltaY();
            if (!NeoForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Pre(s, x, y, dx, dy)).isCanceled()) {
                s.mouseScrolled(x, y, dx, dy);
                NeoForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Post(s, x, y, dx, dy));
            }
        } finally {
            end();
        }
    }

    private static SafeZoneState.Rect begin() {
        SafeZoneState.Rect r = SafeZoneState.computeCurrent();
        SafeZoneState.enter(r);
        redispatching = true;
        return r;
    }

    private static void end() {
        redispatching = false;
        SafeZoneState.exit();
    }
}
