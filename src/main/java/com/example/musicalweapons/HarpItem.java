package com.example.musicalweapons;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class HarpItem extends Item {
    public final float noteDamage;
    public final double noteRange;
    public final int attackCooldown;
    public final double knockbackStrength;
    public final int absorptionAmplifier;
    public final int invisDuration;
    public final int glowDuration;
    public final double glowRadius;
    public final int speedAmplifier;

    public HarpItem(Settings settings, float noteDamage, double noteRange, int attackCooldown,
                    double knockbackStrength, int absorptionAmplifier,
                    int invisDuration, int glowDuration, double glowRadius, int speedAmplifier) {
        super(settings);
        this.noteDamage = noteDamage;
        this.noteRange = noteRange;
        this.attackCooldown = attackCooldown;
        this.knockbackStrength = knockbackStrength;
        this.absorptionAmplifier = absorptionAmplifier;
        this.invisDuration = invisDuration;
        this.glowDuration = glowDuration;
        this.glowRadius = glowRadius;
        this.speedAmplifier = speedAmplifier;
    }

    public int getMode(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("HarpMode") : 0;
    }

    public void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt("HarpMode", mode);
    }

    private long getRemainingCooldown(World world, ItemStack stack, int mode) {
        if (!stack.hasTag()) {
            return 0;
        }
        long readyTime = stack.getTag().getLong("CooldownMode_" + mode);
        long currentTime = world.getTime();
        return Math.max(0, readyTime - currentTime);
    }

    private void setSkillCooldown(World world, ItemStack stack, int mode, int ticks) {
        stack.getOrCreateTag().putLong("CooldownMode_" + mode, world.getTime() + ticks);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int currentMode = getMode(stack);
        int nextMode = user.isSneaking() ? (currentMode + 2) % 3 : (currentMode + 1) % 3;
        setMode(stack, nextMode);

        if (!world.isClient) {
            long remTicks = getRemainingCooldown(world, stack, nextMode);
            String status = remTicks > 0
                    ? String.format(" (КД: %.1f сек)", remTicks / 20.0)
                    : " (Готово!)";

            Text modeText;
            if (nextMode == 0) {
                modeText = new LiteralText("♪ Навык: Атака" + status).formatted(Formatting.RED);
            } else if (nextMode == 1) {
                modeText = new LiteralText("♪ Навык: Щит и отталкивание" + status).formatted(Formatting.GOLD);
            } else {
                modeText = new LiteralText("♪ Навык: Скрытность и разведка" + status).formatted(Formatting.AQUA);
            }
            user.sendMessage(modeText, true);

            float pitch = user.isSneaking() ? 0.8f : 1.2f;
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1.0f, pitch);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    public void activateSkill(World world, PlayerEntity player, ItemStack stack) {
        if (world.isClient) {
            return;
        }

        int mode = getMode(stack);
        long remTicks = getRemainingCooldown(world, stack, mode);

        if (remTicks > 0) {
            player.sendMessage(
                    new LiteralText(String.format("⏳ Навык перезаряжается: %.1f сек", remTicks / 20.0)).formatted(Formatting.GRAY),
                    true
            );
            return;
        }

        if (mode == 0) {
            RedNoteEntity note = new RedNoteEntity(world, player, noteDamage, noteRange);
            note.setProperties(player, player.pitch, player.yaw, 0.0f, 1.8f, 0.0f);
            world.spawnEntity(note);

            world.playSound(null, player.getBlockPos(), MusicalWeaponsMod.HARP_ATTACK_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);
            setSkillCooldown(world, stack, 0, attackCooldown);

        } else if (mode == 1) {
            Box area = player.getBoundingBox().expand(knockbackStrength);
            List<Entity> entities = world.getOtherEntities(player, area);

            for (Entity entity : entities) {
                if (entity instanceof LivingEntity) {
                    Vec3d pushDir = entity.getPos().subtract(player.getPos()).normalize();
                    entity.addVelocity(pushDir.x * (knockbackStrength * 0.45), 0.4, pushDir.z * (knockbackStrength * 0.45));
                    entity.velocityModified = true;
                }
            }

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, absorptionAmplifier));
            world.playSound(null, player.getBlockPos(), MusicalWeaponsMod.HARP_SHIELD_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (world instanceof ServerWorld) {
                ((ServerWorld) world).spawnParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 1.5, 0.5, 1.5, 1.0);
            }
            setSkillCooldown(world, stack, 1, 200);

        } else if (mode == 2) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, invisDuration, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, invisDuration, speedAmplifier));

            Box scanArea = player.getBoundingBox().expand(glowRadius);
            List<Entity> targets = world.getOtherEntities(player, scanArea);
            for (Entity target : targets) {
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, glowDuration, 0));
                }
            }

            world.playSound(null, player.getBlockPos(), MusicalWeaponsMod.HARP_STEALTH_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);
            setSkillCooldown(world, stack, 2, 300);
        }

        stack.damage(1, player, (p) -> p.sendToolBreakStatus(Hand.MAIN_HAND));
    }
}
