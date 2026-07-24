package com.nullblock.mod.block.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.nullblock.mod.NullBlockMod;
import com.nullblock.mod.block.NullBlock;

/**
 * Darkens the camera's view while the player's eyes are physically inside a
 * NullBlock that currently has a visually solid disguise.
 *
 * NullBlock is always fully passable (see NullBlock#getCollisionShape,
 * always Shapes.empty()), so the player's eyes can end up inside a block
 * that LOOKS completely solid (e.g. disguised as stone or wood) without
 * vanilla's own suffocation/in-wall darkening ever kicking in — vanilla
 * only darkens the screen based on real collision/occlusion at the eye
 * position, and NullBlock reports empty collision there.
 *
 * Implemented via ViewportEvent (ComputeFogColor + RenderFog) rather than a
 * GUI overlay layer: ViewportEvent has been part of Forge's client event API
 * since 1.19.3 and remains available in 1.21.4, whereas the GUI-layer
 * registration API (RegisterGuiLayersEvent) that replaced the old
 * RenderGuiOverlayEvent in NeoForge is not confirmed to exist under the same
 * package/class in this Forge version — using it produced a
 * "cannot find symbol" compile error. Forcing black fog with a very short
 * view distance reproduces the same "can't see anything, screen is dark"
 * effect without depending on that uncertain API.
 */
@Mod.EventBusSubscriber(modid = NullBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class NullBlockCameraOverlay {

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isEyeInsideDisguisedNullBlock()) {
            return;
        }
        // Pure black fog color.
        event.setRed(0.0F);
        event.setGreen(0.0F);
        event.setBlue(0.0F);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isEyeInsideDisguisedNullBlock()) {
            return;
        }
        // Push the fog as close to the camera as possible so the black fog
        // color set above covers the entire view, the same visual result as
        // vanilla's "suffocating inside a block" darkening.
        event.setNearPlaneDistance(-8.0F);
        event.setFarPlaneDistance(0.75F);
        event.setCanceled(true);
    }

    private static boolean isEyeInsideDisguisedNullBlock() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return false;
        }

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
        // (rather than just "has any disguise") avoids darkening the view
        // for disguises that aren't full/opaque blocks (glass, slabs,
        // etc.), matching how vanilla only darkens when the eye position is
        // actually inside solid, opaque geometry.
        return disguise.isSolidRender();
    }
}
