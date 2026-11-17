package com.example.musicalweapons;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RedNoteEntity extends ThrownItemEntity implements FlyingItemEntity {
    private float damage = 6.0f;
    private double maxRange = 10.0;
    private Vec3d startPos;

    public RedNoteEntity(EntityType<? extends RedNoteEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
    }

    public RedNoteEntity(World world, LivingEntity owner, float damage, double maxRange) {
        super(MusicalWeaponsMod.RED_NOTE_ENTITY, owner, world);
        this.damage = damage;
        this.maxRange = maxRange;
        this.startPos = owner.getPos();
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return MusicalWeaponsMod.RED_NOTE_ITEM;
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(MusicalWeaponsMod.RED_NOTE_ITEM);
    }

    @Override
    public Packet<?> createSpawnPacket() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(this.getEntityId());
        buf.writeUuid(this.getUuid());
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());
        buf.writeByte((byte) ((int) (this.pitch * 256.0F / 360.0F)));
        buf.writeByte((byte) ((int) (this.yaw * 256.0F / 360.0F)));
        return ServerPlayNetworking.createS2CPacket(MusicalWeaponsMod.SPAWN_NOTE_PACKET, buf);
    }

    @Override
    public void tick() {
        Vec3d velocity = this.getVelocity();
        super.tick();
        this.setVelocity(velocity);

        if (this.world.isClient) {
            this.world.addParticle(ParticleTypes.NOTE, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
        } else {
            if (this.startPos != null && this.getPos().distanceTo(this.startPos) >= this.maxRange) {
                this.remove();
            }
            if (this.age > 100) {
                this.remove();
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (!this.world.isClient) {
            Entity target = entityHitResult.getEntity();
            Entity owner = this.getOwner();
            target.damage(DamageSource.thrownProjectile(this, owner), this.damage);
            this.remove();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (!this.world.isClient) {
            this.remove();
        }
    }
}
