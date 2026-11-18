package com.example.musicalweapons;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ViolinBowEntity extends ThrownItemEntity {
    private Vec3d startPos;
    private ItemStack violinStack = ItemStack.EMPTY;

    public ViolinBowEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
    }

    public ViolinBowEntity(World world, LivingEntity owner, ItemStack violinStack) {
        super(MusicalWeaponsMod.VIOLIN_BOW_ENTITY, owner, world);
        this.startPos = owner.getCameraPosVec(1.0F);
        this.violinStack = violinStack;
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return MusicalWeaponsMod.VIOLIN_BOW_ITEM;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.world.isClient) {
            if (this.world instanceof ServerWorld) {
                ((ServerWorld) this.world).spawnParticles(ParticleTypes.NOTE, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
            }

            if (this.startPos != null && this.getPos().distanceTo(this.startPos) >= 15.0) {
                returnBowToOwner(true);
                this.remove();
            }
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        if (!this.world.isClient) {
            if (this.getOwner() instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) this.getOwner();
                Vec3d hitPos = hitResult.getPos();

                player.requestTeleport(hitPos.x, hitPos.y + 0.2, hitPos.z);
                player.fallDistance = 0.0F;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1));

                this.world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
                if (this.world instanceof ServerWorld) {
                    ((ServerWorld) this.world).spawnParticles(ParticleTypes.PORTAL, hitPos.x, hitPos.y + 0.5, hitPos.z, 20, 0.3, 0.5, 0.3, 0.1);
                }
            }

            returnBowToOwner(false);
            this.remove();
        }
    }

    private void returnBowToOwner(boolean dissipated) {
        if (this.getOwner() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) this.getOwner();

            if (!this.violinStack.isEmpty()) {
                ViolinItem.setBowThrown(this.violinStack, false);
            }

            for (int i = 0; i < player.inventory.size(); i++) {
                ItemStack invStack = player.inventory.getStack(i);
                if (invStack.getItem() instanceof ViolinItem && ViolinItem.isBowThrown(invStack)) {
                    ViolinItem.setBowThrown(invStack, false);
                }
            }

            if (dissipated) {
                player.sendMessage(new LiteralText("♪ Смычок рассеялся на 15 блоках и вернулся к скрипке").formatted(Formatting.YELLOW), true);
                this.world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.0f, 1.8f);
            }
        }
    }

    @Override
    public Packet<?> createSpawnPacket() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(this.getEntityId());
        buf.writeUuid(this.getUuid());
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());
        buf.writeByte(MathHelper.floor(this.pitch * 256.0F / 360.0F));
        buf.writeByte(MathHelper.floor(this.yaw * 256.0F / 360.0F));
        return ServerPlayNetworking.createS2CPacket(MusicalWeaponsMod.SPAWN_BOW_PACKET, buf);
    }
}
