package com.nullblock.mod.block;

import com.nullblock.mod.block.entity.NullBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block that is always fully passable (no collision, no selection-blocking
 * physical interaction) regardless of what it visually disguises itself as.
 *
 * Placing a normal block item on it (right-click with e.g. Stone in hand)
 * assigns that block as the disguise: the null block will then LOOK like
 * that block (for rendering purposes via its BlockEntity), while remaining
 * 100% non-solid. This works for any block registered in the game.
 *
 * Other mods can drive this programmatically through {@link com.nullblock.mod.api.NullBlockAPI}.
 */
public class NullBlock extends Block implements EntityBlock {

    public NullBlock(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // Passability: this is what makes the block always "null" to physics.
    // ------------------------------------------------------------------

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Selection/outline shape can stay a full cube so the player can still
        // target it with a crosshair to place a disguise on it; it does not
        // affect movement since getCollisionShape is what governs physics.
        return Shapes.block();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    // ------------------------------------------------------------------
    // Rendering: delegate to the BlockEntityRenderer using disguise state.
    // The base block itself renders nothing (invisible model), the disguise
    // visual is drawn by NullBlockEntityRenderer on the client.
    // ------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, net.minecraft.core.Direction direction) {
        return false;
    }

    // ------------------------------------------------------------------
    // BlockEntity plumbing
    // ------------------------------------------------------------------

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NullBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    // ------------------------------------------------------------------
    // Interaction: right-clicking with a BlockItem assigns the disguise.
    // Right-clicking empty-handed (or with a non-block item) clears it.
    // ------------------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof NullBlockEntity nullBe)) {
            return ItemInteractionResult.FAIL;
        }

        BlockState disguise = blockItem.getBlock().defaultBlockState();

        if (!level.isClientSide) {
            nullBe.setDisguiseState(disguise);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NullBlockEntity nullBe && nullBe.hasDisguise()) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    nullBe.setDisguiseState(null);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
