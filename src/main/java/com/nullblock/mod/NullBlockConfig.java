package com.nullblock.mod;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common config for NullBlock.
 *
 * showInCreativeMenu:
 *   When true (default), the Null Block item shows up in the Building Blocks
 *   creative tab, same as any normal block.
 *
 *   When false, the item is still fully registered (so ItemStacks, saves,
 *   and other mods' references to ModBlocks.NULL_BLOCK / NULL_BLOCK_ITEM keep
 *   working exactly the same), it is just excluded from the creative menu
 *   listing. This is meant for the case where another mod depends on
 *   NullBlockMod purely as a library — e.g. calling
 *   {@link com.nullblock.mod.api.NullBlockAPI#makePassable} to silently swap
 *   out surface blocks (like grass) for a disguised null block — and does not
 *   want players to be able to obtain/place raw null blocks by hand from
 *   the creative inventory.
 */
public final class NullBlockConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_IN_CREATIVE_MENU;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        SHOW_IN_CREATIVE_MENU = builder
                .comment(
                        "If true, the Null Block item appears in the Building Blocks creative tab.",
                        "Set to false if you only want other mods to be able to place Null Blocks",
                        "programmatically via NullBlockAPI, without players being able to grab it",
                        "from the creative inventory by hand."
                )
                .define("showInCreativeMenu", true);
        builder.pop();

        SPEC = builder.build();
    }

    private NullBlockConfig() {
    }
}
