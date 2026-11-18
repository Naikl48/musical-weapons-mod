package com.example.musicalweapons;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TrumpetItem extends Item {

    public TrumpetItem(Settings settings) {
        super(settings);
    }

    // 0 = Атака, 1 = Лечение
    public static int getMode(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("TrumpetMode") : 0;
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt("TrumpetMode", mode);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int nextMode = (getMode(stack) + 1) % 2;
        setMode(stack, nextMode);

        if (!world.isClient) {
            Text modeText = (nextMode == 0)
                    ? new LiteralText("♪ Режим трубы: Атака (прицел 2 сек)").formatted(Formatting.RED)
                    : new LiteralText("♪ Режим трубы: Лечение (прицел 4 сек)").formatted(Formatting.GREEN);
            user.sendMessage(modeText, true);

            float pitch = (nextMode == 0) ? 1.0f : 1.4f;
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.PLAYERS, 1.0f, pitch);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    public void shoot(World world, PlayerEntity player, ItemStack stack, int chargeTicks) {
        if (world.isClient || player.getItemCooldownManager().isCoolingDown(this)) {
            return;
        }

        int mode = getMode(stack);
        int requiredTicks = (mode == 1) ? 80 : 40;
        boolean isFullyCharged = chargeTicks >= requiredTicks;

        // Рабочий звук трубы для 1.16.5 (комбинация баса и флейты)
        float pitch = isFullyCharged ? 0.8f : 1.3f;
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.5f, pitch);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.PLAYERS, 1.2f, pitch);

        // Луч на 50 блоков
        double maxDistance = 50.0;
        Vec3d startVec = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d endVec = startVec.add(lookVec.multiply(maxDistance));
        Box box = player.getBoundingBox().stretch(lookVec.multiply(maxDistance)).expand(1.0D);

        EntityHitResult hitResult = ProjectileUtil.raycast(
                player, startVec, endVec, box,
                entity -> !entity.isSpectator() && entity.collides(),
                maxDistance * maxDistance
        );

        // Частицы нот вдоль линии выстрела
        double actualDistance = (hitResult != null) ? startVec.distanceTo(hitResult.getPos()) : maxDistance;
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            for (double d = 1.0; d < actualDistance; d += 1.5) {
                Vec3d pPos = startVec.add(lookVec.multiply(d));
                serverWorld.spawnParticles(ParticleTypes.NOTE, pPos.x, pPos.y, pPos.z, 1, 0, 0, 0, 0);
            }
        }

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) hitResult.getEntity();

            if (mode == 1) {
                if (target instanceof PlayerEntity || (target instanceof TameableEntity && ((TameableEntity) target).isTamed())) {
                    float healAmount = isFullyCharged ? 10.0F : 1.0F;
                    target.heal(healAmount);
                    if (world instanceof ServerWorld) {
                        ((ServerWorld) world).spawnParticles(ParticleTypes.HEART, target.getX(), target.getEyeY(), target.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            } else {
                float damage = isFullyCharged ? 10.0F : 2.0F;
                target.damage(DamageSource.player(player), damage);
            }
        }

        player.getItemCooldownManager().set(this, 10);
        stack.damage(1, player, (p) -> p.sendToolBreakStatus(Hand.MAIN_HAND));
    }
}
