package com.nullblock.mod.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the "disguise" appearance for a NullBlock by re-using the vanilla
 * chunk-batch block renderer (BlockRenderDispatcher#renderBatched) against
 * the disguise BlockState, positioned at this block entity's actual world
 * position. The null block's own model is invisible (RenderShape.INVISIBLE);
 * this BER is what actually puts the borrowed appearance on screen — complete
 * with the same ambient occlusion and neighbor-aware shading a real placed
 * block would have, so the disguise blends seamlessly instead of looking like
 * a floating cutout with visible seams against neighboring blocks.
 * Collision/interaction remain unaffected — this is a pure visual layer.
 */
public class NullBlockEntityRenderer implements BlockEntityRenderer<NullBlockEntity> {

    public NullBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NullBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState disguise = blockEntity.getDisguiseState();
        Level level = blockEntity.getLevel();
        if (disguise == null || level == null) {
            return;
        }

        RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(disguise);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        // The poseStack passed into a BER's render() is already translated to
        // this block entity's position (per BER contract). renderBatched()
        // expects an UN-translated stack and applies the given BlockPos itself,
        // so we undo the BER's built-in translation first to avoid rendering
        // the disguise offset by double its own position.
        poseStack.translate(-blockEntity.getBlockPos().getX(),
                -blockEntity.getBlockPos().getY(),
                -blockEntity.getBlockPos().getZ());

        Minecraft.getInstance().getBlockRenderer().renderBatched(
                disguise,
                blockEntity.getBlockPos(),
                level,
                poseStack,
                consumer,
                true,
                RandomSource.create(42L)
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
