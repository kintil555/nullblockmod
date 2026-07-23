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
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState disguise = blockEntity.getDisguiseState();
        BlockState stateToRender;

        if (disguise != null && disguise.getRenderShape() == RenderShape.MODEL) {
            // Has a disguise: render the disguise's own appearance.
            stateToRender = disguise;
        } else if (disguise == null) {
            // No disguise set: fall back to rendering the null block's OWN
            // model (the "X" placeholder texture) as an in-world indicator,
            // instead of staying fully invisible. This model is normally
            // skipped in-world because NullBlock#getRenderShape() reports
            // INVISIBLE (so vanilla doesn't double-render it); we render it
            // here manually via the BER, using the block's own default state
            // rather than the disguise.
            stateToRender = com.nullblock.mod.block.ModBlocks.NULL_BLOCK.get().defaultBlockState();
        } else {
            // Disguise exists but isn't a normal model-rendered block
            // (e.g. air, or something with its own special renderer) —
            // nothing sensible to draw.
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        // getBlockModel() resolves via BlockModelShaper#stateToModelLocation,
        // which maps a BlockState to its BLOCKSTATE-variant model (matching
        // blockstates/null_block.json's variant key), not the item/inventory
        // model. Using ModelResourceLocation(..., "inventory") here previously
        // pointed at a variant that was never registered for a block model,
        // which resolved to the missing-texture model (purple/black).
        BakedModel model = dispatcher.getBlockModel(stateToRender);

        poseStack.pushPose();
        // Render using the REAL level/pos so biome color (tint) and AO are
        // sampled from actual neighbors, not a detached fake context. This
        // gives correct grass tint per biome and correct AO shading against
        // surrounding blocks.
        for (RenderType type : model.getRenderTypes(stateToRender, blockEntity.getLevel().random, net.minecraftforge.client.model.data.ModelData.EMPTY)) {
            dispatcher.getModelRenderer().tesselateBlock(
                    blockEntity.getLevel(),
                    model,
                    stateToRender,
                    blockEntity.getBlockPos(),
                    poseStack,
                    bufferSource.getBuffer(type),
                    true,
                    net.minecraft.util.RandomSource.create(),
                    stateToRender.getSeed(blockEntity.getBlockPos()),
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
