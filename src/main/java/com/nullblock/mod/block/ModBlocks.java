package com.nullblock.mod.block;

import com.nullblock.mod.NullBlockMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, NullBlockMod.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NullBlockMod.MODID);

    public static final RegistryObject<Block> NULL_BLOCK = BLOCKS.register("null_block",
            () -> new NullBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK,
                            ResourceLocation.fromNamespaceAndPath(NullBlockMod.MODID, "null_block")))
                    .mapColor(MapColor.NONE)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
            ));

    public static final RegistryObject<Item> NULL_BLOCK_ITEM = ITEMS.register("null_block",
            () -> new BlockItem(NULL_BLOCK.get(), new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath(NullBlockMod.MODID, "null_block")))
                    .useBlockDescriptionPrefix()));
}
