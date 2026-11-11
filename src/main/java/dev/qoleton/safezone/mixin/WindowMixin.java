package dev.qoleton.safezone.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.qoleton.safezone.SafeZoneState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While the safe zone is "entered" (HUD rendering, or a constrained menu rendering / handling input),
 * anything that asks the window how big the GUI is gets the safe-zone size instead.
 * This is what makes vanilla HUD elements and other mods' overlays lay themselves out inside the zone.
 */
@Mixin(Window.class)
public abstract class WindowMixin {

    @Inject(method = "getGuiScaledWidth", at = @At("HEAD"), cancellable = true)
    private void safezone$guiScaledWidth(CallbackInfoReturnable<Integer> cir) {
        if (SafeZoneState.isEntered()) {
            cir.setReturnValue(SafeZoneState.current().w());
        }
    }

    @Inject(method = "getGuiScaledHeight", at = @At("HEAD"), cancellable = true)
    private void safezone$guiScaledHeight(CallbackInfoReturnable<Integer> cir) {
        if (SafeZoneState.isEntered()) {
            cir.setReturnValue(SafeZoneState.current().h());
        }
    }
}
