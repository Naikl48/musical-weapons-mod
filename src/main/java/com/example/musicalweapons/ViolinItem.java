package com.example.musicalweapons;

import net.minecraft.entity.Entity;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ViolinItem extends Item {

    public ViolinItem(Settings settings) {
        super(settings);
    }

    public static int getMode(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("ViolinMode") : 0;
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt("ViolinMode", mode);
    }

    public static boolean isBowThrown(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("BowThrown");
    }

    public static void setBowThrown(ItemStack stack, boolean thrown) {
        stack.getOrCreateTag().putBoolean("BowThrown", thrown);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (isBowThrown(stack)) {
            return TypedActionResult.fail(stack);
        }

        int nextMode = (getMode(stack) + 1) % 2;
        setMode(stack, nextMode);

        if (!world.isClient) {
            Text modeText = (nextMode == 0)
                    ? new LiteralText("♪ Режим скрипки: Рывок (зажать до 5 сек)").formatted(Formatting.AQUA)
                    : new LiteralText("♪ Режим скрипки: Бросок смычка (зажать 10 сек)").formatted(Formatting.LIGHT_PURPLE);
            user.sendMessage(modeText, true);

            float pitch = (nextMode == 0) ? 1.2f : 1.6f;
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1.0f, pitch);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    public void releaseSkill(World world, PlayerEntity player, ItemStack stack, int chargeTicks) {
        if (world.isClient || player.getItemCooldownManager().isCoolingDown(this) || isBowThrown(stack)) {
            return;
        }

        int mode = getMode(stack);

        if (mode == 0) {
            int clampedTicks = Math.min(chargeTicks, 100);
            float ratio = clampedTicks / 100.0f;
            boolean isFullCharge = clampedTicks >= 100;

            double targetBlocks = 2.0 + (5.0 * ratio);
            double velocityStrength = 0.65 + (targetBlocks - 2.0) * 0.27;

            Vec3d lookVec = player.getRotationVec(1.0F).normalize();
            player.setVelocity(lookVec.multiply(velocityStrength));
            player.velocityModified = true;
            player.fallDistance = 0.0F;

            if (isFullCharge) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 600, 0, false, false, true));
                stack.getOrCreateTag().putBoolean("ViolinSlowFall", true);
                stack.getOrCreateTag().putInt("ViolinAirTicks", 0);
            }

            world.playSound(null, player.getBlockPos(), MusicalWeaponsMod.VIOLIN_DASH_SHOT_SOUND, SoundCategory.PLAYERS, 1.3f, 1.0f);
            if (world instanceof ServerWorld) {
                ((ServerWorld) world).spawnParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.4, 0.4, 0.4, 0.1);
            }

            player.getItemCooldownManager().set(this, 20);
            stack.damage(1, player, (p) -> p.sendToolBreakStatus(Hand.MAIN_HAND));

        } else {
            if (chargeTicks < 200) {
                player.sendMessage(new LiteralText("Смычок не заряжен (нужно 10 сек)!").formatted(Formatting.RED), true);
                return;
            }

            setBowThrown(stack, true);

            ViolinBowEntity bowEntity = new ViolinBowEntity(world, player, stack);
            bowEntity.setProperties(player, player.pitch, player.yaw, 0.0F, 1.5F, 0.0F);
            world.spawnEntity(bowEntity);

            world.playSound(null, player.getBlockPos(), MusicalWeaponsMod.VIOLIN_THROW_SHOT_SOUND, SoundCategory.PLAYERS, 1.3f, 1.0f);
            player.getItemCooldownManager().set(this, 20);
            stack.damage(1, player, (p) -> p.sendToolBreakStatus(Hand.MAIN_HAND));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity && stack.hasTag()) {
            PlayerEntity player = (PlayerEntity) entity;

            if (stack.getTag().getBoolean("ViolinSlowFall")) {
                int airTicks = stack.getTag().getInt("ViolinAirTicks") + 1;
                stack.getTag().putInt("ViolinAirTicks", airTicks);

                if (airTicks > 3 && player.isOnGround()) {
                    player.removeStatusEffect(StatusEffects.SLOW_FALLING);
                    stack.getTag().putBoolean("ViolinSlowFall", false);
                    stack.getTag().putInt("ViolinAirTicks", 0);
                }
            }
        }
    }
}
