package dev.qoleton.safezone.mixin;

import dev.qoleton.safezone.SafeZoneEvents;
import dev.qoleton.safezone.SafeZoneState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the whole menu draw (background layers, Render.Pre, the screen, tooltips, Render.Post - which is
 * where EMI/JEI draw) so it happens inside the safe zone, with the mouse position shifted to match.
 */
@Mixin(value = ClientHooks.class, remap = false)
public abstract class ClientHooksMixin {
    @Unique
    private static boolean safezone$wrapping;

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private static void safezone$wrap(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (safezone$wrapping || !SafeZoneEvents.constrains(screen)) {
            return;
        }
        ci.cancel();
        SafeZoneState.Rect r = SafeZoneState.computeCurrent();
        g.pose().pushPose();
        g.pose().translate(r.x(), r.y(), 0);
        SafeZoneState.enter(r);
        safezone$wrapping = true;
        try {
            ClientHooks.drawScreen(screen, g, mouseX - r.x(), mouseY - r.y(), partialTick);
        } finally {
            safezone$wrapping = false;
            SafeZoneState.exit();
            g.pose().popPose();
        }
    }
}
