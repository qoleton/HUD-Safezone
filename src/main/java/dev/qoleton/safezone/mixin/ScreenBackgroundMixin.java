package dev.qoleton.safezone.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.qoleton.safezone.SafeZoneConfig;
import dev.qoleton.safezone.SafeZoneState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Menus squeezed into the safe zone would only darken the zone. Depending on the "Menu background" setting,
 * draw the background over the whole screen (FULL), leave it in the zone (FRAME) or skip it (OFF).
 * The title-screen panorama always covers the whole screen.
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Shadow
    public int width;
    @Shadow
    public int height;

    @WrapMethod(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;)V")
    private void safezone$menuBackground(GuiGraphics g, Operation<Void> original) {
        safezone$background(g, () -> original.call(g));
    }

    @WrapMethod(method = "renderTransparentBackground")
    private void safezone$transparentBackground(GuiGraphics g, Operation<Void> original) {
        safezone$background(g, () -> original.call(g));
    }

    @WrapMethod(method = "renderBlurredBackground")
    private void safezone$blur(float partialTick, Operation<Void> original) {
        if (SafeZoneState.isEntered() && SafeZoneConfig.get().menuBackground == SafeZoneConfig.MenuBackground.OFF) {
            return;
        }
        original.call(partialTick);
    }

    @WrapMethod(method = "renderPanorama")
    private void safezone$panorama(GuiGraphics g, float partialTick, Operation<Void> original) {
        if (!SafeZoneState.isEntered()) {
            original.call(g, partialTick);
            return;
        }
        safezone$fullScreen(g, () -> original.call(g, partialTick));
    }

    private void safezone$background(GuiGraphics g, Runnable draw) {
        if (!SafeZoneState.isEntered()) {
            draw.run();
            return;
        }
        switch (SafeZoneConfig.get().menuBackground) {
            case OFF -> {
            }
            case FRAME -> draw.run();
            case FULL -> safezone$fullScreen(g, draw);
        }
    }

    /** Run a draw call as if this screen covered the real, whole screen. */
    private void safezone$fullScreen(GuiGraphics g, Runnable draw) {
        int ox = SafeZoneState.offsetX(), oy = SafeZoneState.offsetY();
        int[] real = SafeZoneState.realSize();
        int w = width, h = height;
        g.pose().pushPose();
        g.pose().translate(-ox, -oy, 0);
        SafeZoneState.enterReal();
        width = real[0];
        height = real[1];
        try {
            draw.run();
        } finally {
            width = w;
            height = h;
            SafeZoneState.exit();
            g.pose().popPose();
        }
    }
}
