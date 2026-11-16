package com.example.musicalweapons;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.ActionResult;

public class MusicalWeaponsClient implements ClientModInitializer {
    private boolean wasAttackPressed = false;

    @Override
    public void onInitializeClient() {
        // Отрисовка летящего снаряда-ноты с текстурой red_note.png
        EntityRendererRegistry.INSTANCE.register(MusicalWeaponsMod.RED_NOTE_ENTITY,
                (dispatcher, context) -> new FlyingItemEntityRenderer<>(dispatcher, context.getItemRenderer()));

        // Отменяем копание блоков при клике ЛКМ с арфой
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.getStackInHand(hand).getItem() instanceof HarpItem) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Отменяем обычный удар рукой по мобам при клике ЛКМ с арфой
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.getStackInHand(hand).getItem() instanceof HarpItem) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Отслеживаем нажатие ЛКМ в любой момент (даже в пустой воздух)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.currentScreen == null) {
                boolean isPressed = client.options.keyAttack.isPressed();
                if (isPressed && !wasAttackPressed) {
                    if (client.player.getMainHandStack().getItem() instanceof HarpItem) {
                        ClientPlayNetworking.send(MusicalWeaponsMod.USE_HARP_PACKET, new PacketByteBuf(Unpooled.buffer()));
                    }
                }
                wasAttackPressed = isPressed;
            }
        });
    }
}
