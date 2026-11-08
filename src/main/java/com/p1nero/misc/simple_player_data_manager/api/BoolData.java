package com.p1nero.misc.simple_player_data_manager.api;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** A registered boolean value with a namespaced identity and a lazy default. */
public final class BoolData extends DataKey<Boolean> {
    private BoolData(ResourceLocation id, boolean defaultValue, SyncMode mode) {
        super(id, defaultValue, mode, Tag.TAG_BYTE, value -> ByteTag.valueOf(value), tag -> ((ByteTag) tag).getAsByte() != 0);
        encode(defaultValue);
    }

    public static BoolData create(ResourceLocation id) { return create(id, false); }

    public static BoolData create(ResourceLocation id, boolean defaultValue) {
        return create(id, defaultValue, SyncMode.OWNER);
    }

    public static BoolData create(ResourceLocation id, boolean defaultValue, SyncMode mode) {
        return PlayerDataManager.register(new BoolData(id, defaultValue, mode));
    }

    public boolean toggle(Player player) { return update(player, value -> !value); }
    public boolean markTrue(Player player) { return set(player, true); }
    public boolean markFalse(Player player) { return set(player, false); }
}
