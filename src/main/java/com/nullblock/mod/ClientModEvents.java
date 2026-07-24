package com.nullblock.mod;

import com.nullblock.mod.block.ModBlockEntities;
import com.nullblock.mod.block.ModBlocks;
import com.nullblock.mod.block.entity.NullBlockCameraOverlay;
import com.nullblock.mod.block.entity.NullBlockEntityRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiLayersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = NullBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.NULL_BLOCK_ENTITY.get(), NullBlockEntityRenderer::new);
    }

    // Registers the full-screen darkening layer used when the camera's eye
    // position is inside a NullBlock with a visually solid disguise. Placed
    // above everything else so it darkens the entire HUD/world view, the
    // same visual weight as vanilla's own "suffocating inside a block"
    // darkening.
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(NullBlockMod.MODID, "null_block_camera_overlay"),
                new NullBlockCameraOverlay());
    }

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
