package com.example.musicalweapons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MusicalWeaponsMod implements ModInitializer {
    public static final String MOD_ID = "musicalweapons";
    public static final Identifier USE_HARP_PACKET = new Identifier(MOD_ID, "use_harp");
    public static final Identifier USE_TRUMPET_PACKET = new Identifier(MOD_ID, "use_trumpet");
    public static final Identifier USE_VIOLIN_PACKET = new Identifier(MOD_ID, "use_violin");
    public static final Identifier SPAWN_NOTE_PACKET = new Identifier(MOD_ID, "spawn_note");
    public static final Identifier SPAWN_BOW_PACKET = new Identifier(MOD_ID, "spawn_bow");

    public static final SoundEvent HARP_ATTACK_SOUND = registerSound("harp_attack");
    public static final SoundEvent HARP_SHIELD_SOUND = registerSound("harp_shield");
    public static final SoundEvent HARP_STEALTH_SOUND = registerSound("harp_stealth");
    public static final SoundEvent VIOLIN_DASH_CHARGE_SOUND = registerSound("violin_dash_charge");
    public static final SoundEvent VIOLIN_DASH_SHOT_SOUND = registerSound("violin_dash_shot");
    public static final SoundEvent VIOLIN_THROW_CHARGE_SOUND = registerSound("violin_throw_charge");
    public static final SoundEvent VIOLIN_THROW_SHOT_SOUND = registerSound("violin_throw_shot");

    private static SoundEvent registerSound(String name) {
        Identifier id = new Identifier(MOD_ID, name);
        return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }

    public static final Item RED_NOTE_ITEM = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "red_note"), new Item(new Item.Settings())
    );

    public static final Item VIOLIN_BOW_ITEM = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "violin_bow"), new Item(new Item.Settings())
    );

    public static final EntityType<RedNoteEntity> RED_NOTE_ENTITY = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(MOD_ID, "red_note"),
            FabricEntityTypeBuilder.<RedNoteEntity>create(SpawnGroup.MISC, RedNoteEntity::new)
                    .dimensions(EntityDimensions.fixed(0.35f, 0.35f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(10)
                    .build()
    );

    public static final EntityType<ViolinBowEntity> VIOLIN_BOW_ENTITY = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(MOD_ID, "violin_bow"),
            FabricEntityTypeBuilder.<ViolinBowEntity>create(SpawnGroup.MISC, ViolinBowEntity::new)
                    .dimensions(EntityDimensions.fixed(0.35f, 0.35f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(10)
                    .build()
    );

    public static final Item WOODEN_HARP = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "wooden_harp"),
            new HarpItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(100),
                    6.0f, 10.0, 30,
                    3.0, 1,
                    100, 200, 10.0, 0
            )
    );

    public static final Item IRON_HARP = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "iron_harp"),
            new HarpItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(250),
                    8.0f, 15.0, 20,
                    5.0, 2,
                    200, 300, 15.0, 1
            )
    );

    public static final Item GOLDEN_HARP = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "golden_harp"),
            new HarpItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(500),
                    10.0f, 30.0, 20,
                    7.0, 3,
                    400, 400, 30.0, 2
            )
    );

    public static final Item TRUMPET = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "trumpet"),
            new TrumpetItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(300))
    );

    public static final Item VIOLIN = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "violin"),
            new ViolinItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(350))
    );

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(USE_HARP_PACKET, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof HarpItem) {
                    ((HarpItem) stack.getItem()).activateSkill(player.world, player, stack);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(USE_TRUMPET_PACKET, (server, player, handler, buf, responseSender) -> {
            int chargeTicks = buf.readInt();
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof TrumpetItem) {
                    ((TrumpetItem) stack.getItem()).shoot(player.world, player, stack, chargeTicks);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(USE_VIOLIN_PACKET, (server, player, handler, buf, responseSender) -> {
            int chargeTicks = buf.readInt();
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof ViolinItem) {
                    ((ViolinItem) stack.getItem()).releaseSkill(player.world, player, stack, chargeTicks);
                }
            });
        });
    }
}
