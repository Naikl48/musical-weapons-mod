package com.example.musicalweapons;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RedNoteEntity extends ThrownItemEntity {
    private float damage = 6.0f;
    private double maxRange = 10.0;
    private Vec3d startPos;

    public RedNoteEntity(EntityType<? extends RedNoteEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true); // Отключаем гравитацию
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
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    @Override
    public void tick() {
        Vec3d velocity = this.getVelocity();
        super.tick();
        // Сохраняем постоянную скорость без торможения и падения
        this.setVelocity(velocity);

        if (this.world.isClient) {
            this.world.addParticle(ParticleTypes.NOTE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        } else {
            if (this.startPos != null && this.getPos().distanceTo(this.startPos) >= this.maxRange) {
                this.remove(); // Рассеивается при достижении максимальной дальности
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
            this.remove(); // Исчезает при ударе о блок
        }
    }
}
