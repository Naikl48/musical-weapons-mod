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
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MusicalWeaponsMod implements ModInitializer {
    public static final String MOD_ID = "musicalweapons";
    public static final Identifier USE_HARP_PACKET = new Identifier(MOD_ID, "use_harp");

    public static final Item RED_NOTE_ITEM = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "red_note"), new Item(new Item.Settings())
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

    public static final Item WOODEN_HARP = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "wooden_harp"),
            new HarpItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(100),
                    6.0f, 10.0, 30,   // Урон 6, дальность 10, кд 1.5с
                    3.0, 1,           // Отброс 3, +4 сердца
                    100, 200, 10.0, 0 // Невид. 5с, свет 10с (10 бл.), скорость +20%
            )
    );

    public static final Item IRON_HARP = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "iron_harp"),
            new HarpItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(250),
                    8.0f, 15.0, 20,   // Урон 8, дальность 15, кд 1с
                    5.0, 2,           // Отброс 5, +6 сердец
                    200, 300, 15.0, 1 // Невид. 10с, свет 15с (15 бл.), скорость +40%
            )
    );

    public static final Item GOLDEN_HARP = Registry.register(
            Registry.ITEM, new Identifier(MOD_ID, "golden_harp"),
            new HarpItem(new FabricItemSettings().group(ItemGroup.COMBAT).maxDamage(500),
                    10.0f, 30.0, 20,  // Урон 10, дальность 30, кд 1с
                    7.0, 3,           // Отброс 7, +8 сердец
                    400, 400, 30.0, 2 // Невид. 20с, свет 20с (30 бл.), скорость +60%
            )
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
    }
}
