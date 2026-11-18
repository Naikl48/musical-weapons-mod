package com.example.musicalweapons;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class MusicalWeaponsClient implements ClientModInitializer {
    private boolean wasAttackPressed = false;
    public static int trumpetChargeTicks = 0;
    public static boolean isTrumpetAiming = false;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(MusicalWeaponsMod.RED_NOTE_ENTITY,
                (dispatcher, context) -> new FlyingItemEntityRenderer<>(dispatcher, context.getItemRenderer()));

        ClientPlayNetworking.registerGlobalReceiver(MusicalWeaponsMod.SPAWN_NOTE_PACKET, (client, handler, buf, responseSender) -> {
            int entityId = buf.readVarInt();
            UUID uuid = buf.readUuid();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float pitch = (buf.readByte() * 360.0F) / 256.0F;
            float yaw = (buf.readByte() * 360.0F) / 256.0F;

            client.execute(() -> {
                if (client.world != null) {
                    RedNoteEntity entity = new RedNoteEntity(MusicalWeaponsMod.RED_NOTE_ENTITY, client.world);
                    entity.updatePosition(x, y, z);
                    entity.updateTrackedPosition(x, y, z);
                    entity.pitch = pitch;
                    entity.yaw = yaw;
                    entity.setEntityId(entityId);
                    entity.setUuid(uuid);
                    client.world.addEntity(entityId, entity);
                }
            });
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item item = player.getStackInHand(hand).getItem();
            if (item instanceof HarpItem || item instanceof TrumpetItem) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            Item item = player.getStackInHand(hand).getItem();
            if (item instanceof HarpItem || item instanceof TrumpetItem) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.currentScreen == null) {
                boolean isPressed = client.options.keyAttack.isPressed();
                ItemStack mainStack = client.player.getMainHandStack();
                Item mainHandItem = mainStack.getItem();

                if (isPressed && !wasAttackPressed) {
                    if (mainHandItem instanceof HarpItem) {
                        ClientPlayNetworking.send(MusicalWeaponsMod.USE_HARP_PACKET, new PacketByteBuf(Unpooled.buffer()));
                    }
                }

                if (mainHandItem instanceof TrumpetItem && !client.player.getItemCooldownManager().isCoolingDown(mainHandItem)) {
                    if (isPressed) {
                        isTrumpetAiming = true;
                        trumpetChargeTicks++;

                        int mode = TrumpetItem.getMode(mainStack);
                        int reqTicks = (mode == 1) ? 80 : 40;
                        if (trumpetChargeTicks >= reqTicks) {
                            Formatting color = (mode == 1) ? Formatting.GREEN : Formatting.RED;
                            client.player.sendMessage(new LiteralText("★ ПРИЦЕЛ ГОТОВ ★").formatted(color), true);
                        } else {
                            int percent = (int) ((trumpetChargeTicks / (float) reqTicks) * 100);
                            client.player.sendMessage(new LiteralText("Прицеливание: " + percent + "%").formatted(Formatting.YELLOW), true);
                        }
                    } else if (isTrumpetAiming) {
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeInt(trumpetChargeTicks);
                        ClientPlayNetworking.send(MusicalWeaponsMod.USE_TRUMPET_PACKET, buf);

                        isTrumpetAiming = false;
                        trumpetChargeTicks = 0;
                    }
                } else {
                    isTrumpetAiming = false;
                    trumpetChargeTicks = 0;
                }

                wasAttackPressed = isPressed;
            } else {
                isTrumpetAiming = false;
                trumpetChargeTicks = 0;
            }
        });
    }
}
