package com.nullblock.mod;

import com.nullblock.mod.block.ModBlockEntities;
import com.nullblock.mod.block.ModBlocks;
import com.nullblock.mod.block.entity.NullBlockEntityRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterRenderTypesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NullBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NULL_BLOCK_ENTITY.get(), NullBlockEntityRenderer::new);
    }

    // The NullBlock itself is RenderShape.INVISIBLE so it draws nothing when
    // placed in the world (the disguise is handled entirely by the BER).
    // Without an explicit render type, the block's own model/texture — the
    // one used for the item icon, held item, and break particles — has no
    // registered layer to bind against, which is why the held item/inventory
    // icon shows the missing-texture pattern even though the PNG exists and
    // particles (which pull straight from the texture atlas) render fine.
    // Registering CUTOUT here fixes item/GUI/hand rendering without
    // affecting the in-world invisibility.
    @SubscribeEvent
    public static void registerRenderTypes(RegisterRenderTypesEvent event) {
        event.registerRenderType(ModBlocks.NULL_BLOCK.get(), RenderType.cutout());
    }
}
