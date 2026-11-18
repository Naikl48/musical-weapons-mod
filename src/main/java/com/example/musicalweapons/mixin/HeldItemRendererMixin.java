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
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3f;
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

            // Смычок лежит поверх струн (слегка сбоку от корпуса скрипки, чтобы не застревать в текстуре)
            float offsetX = -0.055f * side;
            float offsetY = 0.02f;
            float offsetZ = -0.04f;
            float stroke = 0.0f;

            if (player == this.client.player && MusicalWeaponsClient.isViolinCharging) {
                int mode = ViolinItem.getMode(item);
                float maxTicks = (mode == 1) ? 100.0f : 60.0f;
                float smoothTicks = MusicalWeaponsClient.violinChargeTicks + tickDelta;
                float progress = Math.min(smoothTicks / maxTicks, 1.0f);

                // Частота движений смычка плавно ускоряется по мере зарядки
                float speed = 0.30f + (progress * 1.25f);
                stroke = (float) Math.sin(smoothTicks * speed);

                // Скольжение смычка вдоль собственной оси поперёк струн скрипки
                offsetY += stroke * 0.16f;
                offsetZ += stroke * 0.16f;
            }

            matrices.translate(offsetX, offsetY, offsetZ);
            // Разворачиваем смычок поперёк деки скрипки + лёгкое покачивание кистью при игре
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(85.0f + stroke * 8.0f));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(12.0f * side));
            matrices.scale(0.9f, 0.9f, 0.9f);

            ItemStack bowStack = new ItemStack(MusicalWeaponsMod.VIOLIN_BOW_ITEM);
            ModelTransformation.Mode transformMode = isRightHand ? ModelTransformation.Mode.FIRST_PERSON_RIGHT_HAND : ModelTransformation.Mode.FIRST_PERSON_LEFT_HAND;

            this.renderItem(player, bowStack, transformMode, !isRightHand, matrices, vertexConsumers, light);
            matrices.pop();
        }
    }
}
