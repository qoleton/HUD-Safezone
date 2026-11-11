package dev.qoleton.safezone.mixin.compat;

import dev.qoleton.safezone.SafeZoneState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Xaero's Minimap resets the GUI pose to identity before drawing its HUD, which throws away the safe-zone
 * shift (while still laying out against the zone's size). Re-apply the shift right after that reset.
 * Optional: does nothing when Xaero's Minimap isn't installed.
 */
@Pseudo
@Mixin(targets = "xaero.common.events.ModClientEvents", remap = false)
public abstract class XaeroHudMixin {

    @Inject(method = "handleRenderModOverlay",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;setIdentity()V", shift = At.Shift.AFTER),
            require = 0)
    private void safezone$reapplyShift(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SafeZoneState.isEntered()) {
            guiGraphics.pose().translate(SafeZoneState.offsetX(), SafeZoneState.offsetY(), 0);
        }
    }
}
