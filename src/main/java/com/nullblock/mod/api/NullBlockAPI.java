package com.nullblock.mod.api;

import com.nullblock.mod.block.ModBlocks;
import com.nullblock.mod.block.entity.NullBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Public, stable integration surface for other mods that want to depend on
 * NullBlock. Add this mod as a compile/runtime dependency and call these
 * statics instead of poking at internal classes.
 *
 * Typical uses by other mods:
 *  - Programmatically place a null block during world generation (e.g. to
 *    carve out a fully walk-through "backrooms" pocket dimension — one
 *    example use case, not the purpose of this mod itself).
 *  - Turn any existing block in the world into a passable null block copy
 *    of itself (disguise = the block it replaced).
 *  - Query whether a given BlockState/position is a null block, and what
 *    it's currently disguised as.
 */
public final class NullBlockAPI {

    private NullBlockAPI() {
    }

    /** The registered Null Block instance, for direct comparisons (state.is(NullBlockAPI.block())). */
    public static Block block() {
        return ModBlocks.NULL_BLOCK.get();
    }

    /** Returns true if the given state is a NullBlock (regardless of its current disguise). */
    public static boolean isNullBlock(BlockState state) {
        return state.is(block());
    }

    /**
     * Places a null block at the given position, optionally disguised as another block.
     *
     * @param level          the world/level to place into
     * @param pos            target position
     * @param disguiseAs     block to visually disguise as, or null for no disguise
     * @param notifyNeighbors whether to send the usual block-update notifications (flag 3)
     */
    public static void placeNullBlock(LevelAccessor level, BlockPos pos, @Nullable Block disguiseAs, boolean notifyNeighbors) {
        BlockState state = block().defaultBlockState();
        int flags = notifyNeighbors ? 3 : 2;
        level.setBlock(pos, state, flags);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NullBlockEntity nullBe && disguiseAs != null) {
            nullBe.setDisguiseState(disguiseAs.defaultBlockState());
        }
    }

    /**
     * Convenience overload: replaces whatever block currently occupies {@code pos}
     * with a null block disguised as that same original block. Useful for mods that
     * want to make an existing structure passable without changing its appearance.
     */
    public static void makePassable(LevelAccessor level, BlockPos pos) {
        BlockState original = level.getBlockState(pos);
        placeNullBlock(level, pos, original.getBlock(), true);
    }

    /**
     * Returns the block currently disguised at the given position, or null if the
     * position isn't a null block, has no block entity, or has no disguise set.
     */
    @Nullable
    public static BlockState getDisguiseState(LevelAccessor level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NullBlockEntity nullBe) {
            return nullBe.getDisguiseState();
        }
        return null;
    }

    /**
     * Sets or clears the disguise on an existing null block at the given position.
     * No-op if the position is not a null block.
     */
    public static void setDisguiseState(LevelAccessor level, BlockPos pos, @Nullable BlockState disguise) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NullBlockEntity nullBe) {
            nullBe.setDisguiseState(disguise);
        }
    }
}
