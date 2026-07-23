package com.nullblock.mod.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the "disguise" appearance for a NullBlock by re-using the vanilla
 * block model renderer against the disguise BlockState. The null block's own
 * model is invisible (RenderShape.INVISIBLE); this BER is what actually puts
 * the borrowed appearance on screen. Collision/interaction remain unaffected —
 * this is a pure visual layer.
 *
 * Uses BlockModelRenderer#tesselateBlock against the REAL BlockAndTintGetter
 * and BlockPos of this block entity, so ambient occlusion is computed from
 * actual neighboring blocks and biome tint (e.g. grass color) is sampled from
 * this position's real biome — the disguise blends into its surroundings.
 */
public class NullBlockEntityRenderer implements BlockEntityRenderer<NullBlockEntity> {

    public NullBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NullBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState disguise = blockEntity.getDisguiseState();
        if (disguise == null || blockEntity.getLevel() == null || disguise.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(disguise);

        poseStack.pushPose();
        // Render using the REAL level/pos so biome color (tint) and AO are
        // sampled from actual neighbors, not a detached fake context. This
        // gives correct grass tint per biome and correct AO shading against
        // surrounding blocks.
        for (RenderType type : model.getRenderTypes(disguise, blockEntity.getLevel().random, net.minecraftforge.client.model.data.ModelData.EMPTY)) {
            dispatcher.getModelRenderer().tesselateBlock(
                    blockEntity.getLevel(),
                    model,
                    disguise,
                    blockEntity.getBlockPos(),
                    poseStack,
                    bufferSource.getBuffer(type),
                    true,
                    net.minecraft.util.RandomSource.create(),
                    disguise.getSeed(blockEntity.getBlockPos()),
                    packedOverlay
            );
        }
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
