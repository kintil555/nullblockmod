package com.nullblock.mod.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the "disguise" appearance for a NullBlock by re-using the vanilla
 * block model renderer against the disguise BlockState. The null block's own
 * model is invisible (RenderShape.INVISIBLE); this BER is what actually puts
 * the borrowed appearance on screen. Collision/interaction remain unaffected —
 * this is a pure visual layer.
 */
public class NullBlockEntityRenderer implements BlockEntityRenderer<NullBlockEntity> {

    public NullBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NullBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState disguise = blockEntity.getDisguiseState();
        if (disguise == null || blockEntity.getLevel() == null) {
            return;
        }

        poseStack.pushPose();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                disguise,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(NullBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
