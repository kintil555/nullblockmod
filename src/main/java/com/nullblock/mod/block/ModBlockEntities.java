package com.nullblock.mod.block;

import com.nullblock.mod.NullBlockMod;
import com.nullblock.mod.block.entity.NullBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NullBlockMod.MODID);

    public static final RegistryObject<BlockEntityType<NullBlockEntity>> NULL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("null_block_entity", () -> BlockEntityType.Builder.of(
                    NullBlockEntity::new, ModBlocks.NULL_BLOCK.get()
            ).build(null));
}
