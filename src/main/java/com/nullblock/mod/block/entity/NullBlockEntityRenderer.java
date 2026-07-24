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

        // checkSides=true would ask Block.shouldRenderFace() to compare OUR
        // block (NullBlock; invisible, no real shape) against the raw
        // neighbor BlockState. Two adjacent NullBlocks both report
        // themselves (not their disguise) into that check, so the shared
        // face between two disguised NullBlocks ends up double-drawn
        // (visible seam / z-fighting) instead of culling like vanilla snow
        // layers cull against each other. Fix: wrap the level so
        // getBlockState() on a neighboring NullBlock returns that
        // neighbor's DISGUISE state instead, so Block.shouldRenderFace
        // (called internally by tesselateBlock with checkSides=true) sees
        // disguise-vs-disguise and culls correctly in both directions.
        net.minecraft.world.level.BlockAndTintGetter view = disguiseAwareView(blockEntity.getLevel());

        poseStack.pushPose();
        // Render using a wrapped level/pos so biome color (tint) and AO are
        // still sampled from actual neighbors, but face culling sees each
        // neighboring NullBlock's disguise instead of the invisible shell.
        for (RenderType type : model.getRenderTypes(stateToRender, blockEntity.getLevel().random, net.minecraftforge.client.model.data.ModelData.EMPTY)) {
            dispatcher.getModelRenderer().tesselateBlock(
                    view,
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

    // ------------------------------------------------------------------
    // Culling fix: wraps the real level in a dynamic proxy so that
    // BlockAndTintGetter#getBlockState(pos), when called by vanilla's
    // Block.shouldRenderFace() during tesselateBlock(checkSides=true),
    // returns a neighboring NullBlock's DISGUISE state instead of the
    // invisible NullBlock state itself. Every other method (lighting,
    // biome tint, block entities, fluid state, etc.) is forwarded
    // untouched to the real level, so AO/tint/lighting stay accurate.
    // A java.lang.reflect.Proxy is used instead of hand-implementing every
    // BlockAndTintGetter/BlockGetter method, which avoids having to keep a
    // huge delegate class in sync with the interface across MC updates.
    // ------------------------------------------------------------------
    private static net.minecraft.world.level.BlockAndTintGetter disguiseAwareView(Level realLevel) {
        return (net.minecraft.world.level.BlockAndTintGetter) java.lang.reflect.Proxy.newProxyInstance(
                NullBlockEntityRenderer.class.getClassLoader(),
                new Class<?>[]{net.minecraft.world.level.BlockAndTintGetter.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getBlockState")
                            && args != null && args.length == 1
                            && args[0] instanceof net.minecraft.core.BlockPos pos) {
                        BlockState real = realLevel.getBlockState(pos);
                        if (real.getBlock() instanceof com.nullblock.mod.block.NullBlock) {
                            BlockEntity be = realLevel.getBlockEntity(pos);
                            if (be instanceof NullBlockEntity nullBe && nullBe.hasDisguise()) {
                                return nullBe.getDisguiseState();
                            }
                            // No disguise: treat as air so it never
                            // falsely culls a neighbor's face.
                            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                        }
                        return real;
                    }
                    return method.invoke(realLevel, args);
                }
        );
    }
}
