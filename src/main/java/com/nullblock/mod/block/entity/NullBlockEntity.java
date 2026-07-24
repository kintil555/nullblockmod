package com.nullblock.mod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.nullblock.mod.block.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the "disguise" state for a NullBlock — the appearance the null block
 * borrows from another block, while keeping its own collision/interaction
 * behaviour (fully passable).
 *
 * Other mods can read/write this via {@link com.nullblock.mod.api.NullBlockAPI}
 * instead of touching this class directly.
 */
public class NullBlockEntity extends BlockEntity {

    /** The block state being visually mimicked. Null = no disguise (default invisible/void look). */
    @Nullable
    private BlockState disguiseState;

    public NullBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NULL_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public BlockState getDisguiseState() {
        return disguiseState;
    }

    /**
     * Sets the block this null block should visually mimic. Pass null to clear
     * the disguise. Does not touch collision — the null block is always passable
     * regardless of disguise.
     */
    public void setDisguiseState(@Nullable BlockState state) {
        this.disguiseState = state;
        setChanged();
        if (level != null) {
            // Mirror the disguise's light emission onto our own BlockState's
            // LIGHT_LEVEL property, then force the light engine to recheck
            // this position so torches/frog lights/etc. placed as a disguise
            // actually light up the world, not just look lit.
            int lightValue = state != null
                    ? state.getLightEmission(level, worldPosition)
                    : 0;
            BlockState newState = getBlockState().setValue(com.nullblock.mod.block.NullBlock.LIGHT_LEVEL, lightValue);
            level.setBlock(worldPosition, newState, 3);
            level.getLightEngine().checkBlock(worldPosition);
            level.sendBlockUpdated(worldPosition, newState, newState, 3);

            // NOTE: setDisguiseState() is called from NullBlock#useItemOn
            // on the SERVER (level.isClientSide is false there), so a
            // level.isClientSide-guarded remesh call here would never run.
            // The client instead learns about the new disguise later,
            // through the ClientboundBlockEntityDataPacket this triggers,
            // which invokes onDataPacket() below — that is where the actual
            // client-side neighbor remesh now happens. See remeshNeighbors().
        }
    }

    // ------------------------------------------------------------------
    // Client-side AO fix: force neighboring render chunks to rebuild
    // whenever this block's disguise changes, so vanilla full blocks
    // (grass, sand, stone, etc.) sitting next to a NullBlock immediately
    // pick up correct ambient occlusion against the new disguise instead
    // of keeping stale (pre-disguise) shading until something else
    // happens to touch their chunk.
    // ------------------------------------------------------------------

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                              ClientboundBlockEntityDataPacket packet,
                              HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        // super.onDataPacket() applies the packet's tag via handleUpdateTag()
        // -> loadAdditional(), which is what actually refreshes this.disguiseState
        // on the client. Only after that do we know the new disguise and can
        // remesh neighbors against it.
        remeshNeighborsForAO();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // Chunk (re)load / initial sync path — same reasoning as onDataPacket.
        remeshNeighborsForAO();
    }

    /**
     * Ambient occlusion for a NEIGHBOR's face is baked into that neighbor's
     * own render chunk mesh when vanilla's chunk mesher reads this
     * position's occlusion (NullBlock#getOcclusionShape). That read only
     * happens when the neighbor's render chunk gets remeshed. Changing the
     * disguise does not by itself invalidate a neighboring vanilla block's
     * (already-built) mesh, so it keeps stale AO until something else
     * touches it. ClientLevel#setBlocksDirty (an earlier attempt at this
     * fix) does NOT queue a mesh rebuild — it only updates
     * block-destruction-progress bookkeeping. The method that actually
     * queues a render-chunk rebuild is LevelRenderer#setBlockDirty. Call it
     * for all 26 neighbors — including diagonals, since AO sampling reads
     * the 8 blocks around each vertex — so every affected render chunk
     * rebuilds and picks up the new occlusion state, exactly like vanilla
     * full blocks (e.g. snow layers) already do via their own setBlock
     * calls (which internally call the same method for their 6 face
     * neighbors).
     */
    private void remeshNeighborsForAO() {
        if (level == null || !level.isClientSide) {
            return;
        }
        net.minecraft.client.renderer.LevelRenderer levelRenderer =
                net.minecraft.client.Minecraft.getInstance().levelRenderer;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighbor = worldPosition.offset(dx, dy, dz);
                    BlockState neighborState = level.getBlockState(neighbor);
                    levelRenderer.setBlockDirty(neighbor, neighborState, neighborState);
                }
            }
        }
    }

    public boolean hasDisguise() {
        return disguiseState != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (disguiseState != null) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(disguiseState.getBlock());
            tag.putString("DisguiseBlock", id.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("DisguiseBlock")) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("DisguiseBlock"));
            disguiseState = BuiltInRegistries.BLOCK.getOptional(id)
                    .map(net.minecraft.world.level.block.Block::defaultBlockState)
                    .orElse(null);
        } else {
            disguiseState = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
