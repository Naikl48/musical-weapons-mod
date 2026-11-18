package com.example.musicalweapons.mixin;

import com.example.musicalweapons.MusicalWeaponsClient;
import com.example.musicalweapons.TrumpetItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && MusicalWeaponsClient.isTrumpetAiming) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof TrumpetItem) {
                int mode = TrumpetItem.getMode(stack);
                float maxTicks = (mode == 1) ? 80.0F : 40.0F;

                float progress = MathHelper.clamp((MusicalWeaponsClient.trumpetChargeTicks + tickDelta) / maxTicks, 0.0F, 1.0F);
                double baseFov = cir.getReturnValue();
                cir.setReturnValue(MathHelper.lerp(progress, baseFov, baseFov * 0.4));
            }
        }
    }
}
