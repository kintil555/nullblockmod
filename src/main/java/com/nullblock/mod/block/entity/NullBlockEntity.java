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
