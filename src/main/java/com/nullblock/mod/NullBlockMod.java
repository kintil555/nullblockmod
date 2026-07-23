package com.nullblock.mod;

import com.nullblock.mod.block.ModBlockEntities;
import com.nullblock.mod.block.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NullBlock — a passthrough "phantom" block that can visually disguise itself
 * as any other block in the game while remaining fully non-solid: no collision,
 * no interaction blocking, the player walks straight through it.
 *
 * This mod is built as a LIBRARY / API for other mods to depend on. See
 * {@link com.nullblock.mod.api.NullBlockAPI} for the public integration surface.
 * Programmatically placing null blocks during world generation (e.g. to build
 * a "backrooms"-style noclip zone) is just ONE example use case enabled by the
 * API — it is not what this mod does by itself.
 */
@Mod(NullBlockMod.MODID)
public class NullBlockMod {

    public static final String MODID = "nullblock";
    public static final Logger LOGGER = LoggerFactory.getLogger(NullBlockMod.class);

    public NullBlockMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NullBlockConfig.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS
                && NullBlockConfig.SHOW_IN_CREATIVE_MENU.get()) {
            event.accept(ModBlocks.NULL_BLOCK_ITEM.get());
        }
    }
}
