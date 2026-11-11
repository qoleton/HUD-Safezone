package dev.qoleton.safezone.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.qoleton.safezone.SafeZoneConfig;
import dev.qoleton.safezone.SafeZoneEvents;
import dev.qoleton.safezone.SafeZoneState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wraps the entire HUD draw. Priority is above Gnetum's (1500) so this wrapper sits OUTSIDE Gnetum's
 * HUD caching: every pass Gnetum renders, cached or not, happens inside the safe zone.
 */
@Mixin(value = GameRenderer.class, priority = 2000)
public abstract class GameRendererMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void safezone$wrapHud(Gui gui, GuiGraphics g, DeltaTracker deltaTracker, Operation<Void> original) {
        if (!SafeZoneConfig.get().hudActive()) {
            original.call(gui, g, deltaTracker);
            return;
        }
        SafeZoneState.Rect r = SafeZoneState.computeCurrent();
        g.pose().pushPose();
        g.pose().translate(r.x(), r.y(), 0);
        SafeZoneState.enter(r);
        SafeZoneEvents.hudEntered = true;
        try {
            original.call(gui, g, deltaTracker);
        } finally {
            SafeZoneEvents.hudEntered = false;
            SafeZoneState.exit();
            g.pose().popPose();
        }
    }
}
