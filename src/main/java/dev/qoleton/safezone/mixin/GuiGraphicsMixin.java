package dev.qoleton.safezone.mixin;

import dev.qoleton.safezone.SafeZoneState;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * In 1.21.1 scissor rectangles ignore the pose matrix, so a scrolling list drawn inside the (translated)
 * safe zone would be clipped at its untranslated position. Shift scissor coordinates by the zone offset.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int safezone$minX(int v) {
        return v + SafeZoneState.offsetX();
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int safezone$minY(int v) {
        return v + SafeZoneState.offsetY();
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int safezone$maxX(int v) {
        return v + SafeZoneState.offsetX();
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private int safezone$maxY(int v) {
        return v + SafeZoneState.offsetY();
    }

    @ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int safezone$pointX(int v) {
        return v + SafeZoneState.offsetX();
    }

    @ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int safezone$pointY(int v) {
        return v + SafeZoneState.offsetY();
    }
}
