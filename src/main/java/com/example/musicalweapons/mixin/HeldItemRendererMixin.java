package com.example.musicalweapons.mixin;

import com.example.musicalweapons.MusicalWeaponsClient;
import com.example.musicalweapons.MusicalWeaponsMod;
import com.example.musicalweapons.ViolinItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow @Final private MinecraftClient client;

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", shift = At.Shift.AFTER))
    private void renderViolinBowFirstPerson(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (item.getItem() instanceof ViolinItem && !ViolinItem.isBowThrown(item)) {
            matrices.push();

            boolean isRightHand = (hand == Hand.MAIN_HAND && player.getMainArm() == Arm.RIGHT)
                               || (hand == Hand.OFF_HAND && player.getMainArm() == Arm.LEFT);
            int side = isRightHand ? 1 : -1;

            float offsetX = -0.15f * side;
            float offsetY = 0.05f;
            float offsetZ = -0.05f;

            if (player == this.client.player && MusicalWeaponsClient.isViolinCharging) {
                int mode = ViolinItem.getMode(item);
                float maxTicks = (mode == 1) ? 200.0f : 100.0f;
                float smoothTicks = MusicalWeaponsClient.violinChargeTicks + tickDelta;
                float progress = Math.min(smoothTicks / maxTicks, 1.0f);

                float speed = 0.25f + (progress * 1.15f);
                float slide = (float) Math.sin(smoothTicks * speed) * 0.22f;

                offsetX += slide * side;
                offsetY += slide * 0.12f;
            }

            matrices.translate(offsetX, offsetY, offsetZ);
            matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(75.0f * side));
            matrices.scale(0.85f, 0.85f, 0.85f);

            ItemStack bowStack = new ItemStack(MusicalWeaponsMod.VIOLIN_BOW_ITEM);
            ModelTransformation.Mode transformMode = isRightHand ? ModelTransformation.Mode.FIRST_PERSON_RIGHT_HAND : ModelTransformation.Mode.FIRST_PERSON_LEFT_HAND;

            this.renderItem(player, bowStack, transformMode, !isRightHand, matrices, vertexConsumers, light);
            matrices.pop();
        }
    }
}
