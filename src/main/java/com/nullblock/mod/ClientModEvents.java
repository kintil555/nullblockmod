package com.nullblock.mod;

import com.nullblock.mod.block.ModBlockEntities;
import com.nullblock.mod.block.ModBlocks;
import com.nullblock.mod.block.entity.NullBlockEntityRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = NullBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NULL_BLOCK_ENTITY.get(), NullBlockEntityRenderer::new);
    }

    // NOTE: the "camera inside a disguised NullBlock" darkening effect is
    // registered separately by NullBlockCameraOverlay itself, via its own
    // @Mod.EventBusSubscriber(bus = Bus.FORGE) annotation — it listens to
    // ViewportEvent, not a MOD-bus registration event, so it doesn't need
    // to be wired up here.

    // Explicitly assign the render layer for the NullBlock's own model. This
    // model is used for the item icon, held item, and break particles (the
    // in-world appearance is handled separately by RenderShape.INVISIBLE +
    // the BER). Without this, the block defaults to the SOLID layer, which
    // conflicts with noOcclusion()/no-collision blocks and causes the
    // item/hand render to show the missing-texture pattern.
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.NULL_BLOCK.get(), RenderType.cutout()));
    }
}
