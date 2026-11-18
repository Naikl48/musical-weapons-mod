package com.example.musicalweapons;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class MusicalWeaponsClient implements ClientModInitializer {
    private boolean wasAttackPressed = false;
    public static int trumpetChargeTicks = 0;
    public static boolean isTrumpetAiming = false;

    public static int violinChargeTicks = 0;
    public static boolean isViolinCharging = false;
    private static SoundInstance activeViolinChargeSound = null;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(MusicalWeaponsMod.RED_NOTE_ENTITY,
                (dispatcher, context) -> new FlyingItemEntityRenderer<>(dispatcher, context.getItemRenderer()));

        EntityRendererRegistry.INSTANCE.register(MusicalWeaponsMod.VIOLIN_BOW_ENTITY,
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

        ClientPlayNetworking.registerGlobalReceiver(MusicalWeaponsMod.SPAWN_BOW_PACKET, (client, handler, buf, responseSender) -> {
            int entityId = buf.readVarInt();
            UUID uuid = buf.readUuid();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float pitch = (buf.readByte() * 360.0F) / 256.0F;
            float yaw = (buf.readByte() * 360.0F) / 256.0F;

            client.execute(() -> {
                if (client.world != null) {
                    ViolinBowEntity entity = new ViolinBowEntity(MusicalWeaponsMod.VIOLIN_BOW_ENTITY, client.world);
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
            if (item instanceof HarpItem || item instanceof TrumpetItem || item instanceof ViolinItem) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            Item item = player.getStackInHand(hand).getItem();
            if (item instanceof HarpItem || item instanceof TrumpetItem || item instanceof ViolinItem) {
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

                if (mainHandItem instanceof ViolinItem && !client.player.getItemCooldownManager().isCoolingDown(mainHandItem) && !ViolinItem.isBowThrown(mainStack)) {
                    if (isPressed) {
                        int mode = ViolinItem.getMode(mainStack);

                        if (!isViolinCharging) {
                            SoundEvent chargeSound = (mode == 1)
                                    ? MusicalWeaponsMod.VIOLIN_THROW_CHARGE_SOUND
                                    : MusicalWeaponsMod.VIOLIN_DASH_CHARGE_SOUND;
                            activeViolinChargeSound = PositionedSoundInstance.master(chargeSound, 1.0F);
                            client.getSoundManager().play(activeViolinChargeSound);
                        }

                        isViolinCharging = true;
                        violinChargeTicks++;

                        int reqTicks = (mode == 1) ? 100 : 60;

                        if (mode == 0) {
                            int clamped = Math.min(violinChargeTicks, 60);
                            double dist = 2.0 + 5.0 * (clamped / 60.0);
                            if (clamped >= 60) {
                                client.player.sendMessage(new LiteralText("★ РЫВОК: 7.0 БЛ. + ПЛАВНОЕ ПАДЕНИЕ ★").formatted(Formatting.AQUA), true);
                            } else {
                                client.player.sendMessage(new LiteralText(String.format("Заряд рывка: %.1f / 7.0 бл.", dist)).formatted(Formatting.YELLOW), true);
                            }
                        } else {
                            if (violinChargeTicks >= reqTicks) {
                                client.player.sendMessage(new LiteralText("★ СМЫЧОК ГОТОВ К БРОСКУ ★").formatted(Formatting.LIGHT_PURPLE), true);
                            } else {
                                int percent = (int) ((violinChargeTicks / (float) reqTicks) * 100);
                                client.player.sendMessage(new LiteralText("Подготовка броска: " + percent + "%").formatted(Formatting.YELLOW), true);
                            }
                        }
                    } else if (isViolinCharging) {
                        if (activeViolinChargeSound != null) {
                            client.getSoundManager().stop(activeViolinChargeSound);
                            activeViolinChargeSound = null;
                        }

                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeInt(violinChargeTicks);
                        ClientPlayNetworking.send(MusicalWeaponsMod.USE_VIOLIN_PACKET, buf);

                        isViolinCharging = false;
                        violinChargeTicks = 0;
                    }
                } else {
                    if (activeViolinChargeSound != null) {
                        client.getSoundManager().stop(activeViolinChargeSound);
                        activeViolinChargeSound = null;
                    }
                    isViolinCharging = false;
                    violinChargeTicks = 0;
                }

                wasAttackPressed = isPressed;
            } else {
                if (activeViolinChargeSound != null) {
client.getSoundManager().stop(activeViolinChargeSound);
activeViolinChargeSound = null;
}
isTrumpetAiming = false;
trumpetChargeTicks = 0;
isViolinCharging = false;
violinChargeTicks = 0;
}
});
}
}
