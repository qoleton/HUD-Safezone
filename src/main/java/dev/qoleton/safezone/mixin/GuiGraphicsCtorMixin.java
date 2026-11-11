package dev.qoleton.safezone.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.qoleton.safezone.SafeZoneState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Some mods (e.g. AntOS) build a brand-new GuiGraphics inside their render method, which starts with an
 * empty pose and so loses the safe-zone shift. Give such fresh instances the same shift.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsCtorMixin {
    @Shadow
    @Final
    private PoseStack pose;

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V",
            at = @At("TAIL"))
    private void safezone$applyZoneShift(Minecraft minecraft, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        if (SafeZoneState.isEntered()) {
            pose.translate(SafeZoneState.offsetX(), SafeZoneState.offsetY(), 0);
        }
    }
}
