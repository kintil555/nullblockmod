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

            // Ambient occlusion for a NEIGHBOR's face is baked into that
            // neighbor's own chunk mesh when vanilla's chunk mesher reads
            // this position's occlusion (NullBlock#getOcclusionShape). That
            // read only happens when the neighbor's section gets remeshed.
            // level.setBlock() above only guarantees a remesh of THIS
            // position's own section; changing the disguise here (e.g. from
            // "no disguise" to "grass") does not by itself tell the game
            // that neighboring sections' meshes are now stale, so a
            // neighboring grass block keeps its old (pre-disguise) AO until
            // something else happens to touch it. Explicitly mark every
            // neighboring section dirty — including diagonals, since AO
            // sampling reads the 8 blocks around each vertex, not just the
            // 6 face-adjacent ones — so they rebuild and pick up the new
            // occlusion state immediately, exactly like vanilla full blocks
            // (e.g. snow layers) already do via their own setBlock calls.
            if (level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos neighbor = worldPosition.offset(dx, dy, dz);
                            clientLevel.setBlocksDirty(neighbor, newState, newState);
                        }
                    }
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
