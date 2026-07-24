package com.nullblock.mod.block.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.nullblock.mod.block.NullBlock;

/**
 * Darkens the whole screen while the camera's eye position is physically
 * inside a NullBlock that currently has a visually solid disguise.
 *
 * NullBlock is always fully passable (see NullBlock#getCollisionShape,
 * always Shapes.empty()), so the player's eyes can end up inside a block
 * that LOOKS completely solid (e.g. disguised as stone or wood) without
 * vanilla's own suffocation/in-wall darkening ever kicking in — vanilla
 * only darkens the screen based on real collision/occlusion at the eye
 * position, and NullBlock reports empty collision there. This layer
 * re-creates that "inside a solid block" darkening specifically for the
 * disguise's appearance, independent of the (always passable) collision.
 */
public class NullBlockCameraOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (!isEyeInsideDisguisedNullBlock(mc.player)) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // Fully opaque black — matches the visual weight of vanilla's
        // "suffocating inside a block" full-screen darkening (e.g. being
        // stuck inside a solid block without noclip).
        RenderSystem.enableBlend();
        guiGraphics.fill(0, 0, width, height, 0xFF000000);
        RenderSystem.disableBlend();
    }

    private static boolean isEyeInsideDisguisedNullBlock(Player player) {
        Vec3 eyePos = player.getEyePosition();
        BlockPos eyeBlockPos = BlockPos.containing(eyePos);
        BlockState stateAtEye = player.level().getBlockState(eyeBlockPos);

        if (!(stateAtEye.getBlock() instanceof NullBlock)) {
            return false;
        }

        BlockEntity be = player.level().getBlockEntity(eyeBlockPos);
        if (!(be instanceof NullBlockEntity nullBe) || !nullBe.hasDisguise()) {
            // No disguise: NullBlock renders its own translucent/invisible
            // placeholder, not a solid appearance — nothing to darken for.
            return false;
        }

        BlockState disguise = nullBe.getDisguiseState();

        // Only darken when the disguise LOOKS solid at this exact point.
        // Using the disguise's own "is a solid full-cube render" signal
        // (rather than just "has any disguise") avoids darkening the
        // screen for disguises that aren't full/opaque blocks (glass,
        // slabs, etc.), matching how vanilla only darkens when the eye
        // position is actually inside solid, opaque geometry.
        return isVisuallySolid(disguise);
    }

    private static boolean isVisuallySolid(BlockState disguise) {
        // A full, opaque render shape is the same signal vanilla itself
        // effectively relies on to treat a position as "inside a solid
        // block" for rendering purposes. Cheap and reliable here since we
        // already have the disguise BlockState in hand.
        return disguise.isSolidRender();
    }
}
