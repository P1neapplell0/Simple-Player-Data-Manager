package com.p1nero.misc.simple_player_data_manager.api;

import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** A registered long value with a namespaced identity and a lazy default. */
public final class LongData extends DataKey<Long> {
    private LongData(ResourceLocation id, long defaultValue, SyncMode mode) {
        super(id, defaultValue, mode, Tag.TAG_LONG, LongTag::valueOf, tag -> ((LongTag) tag).getAsLong());
        encode(defaultValue);
    }

    public static LongData create(ResourceLocation id) { return create(id, 0L); }

    public static LongData create(ResourceLocation id, long defaultValue) {
        return create(id, defaultValue, SyncMode.OWNER);
    }

    public static LongData create(ResourceLocation id, long defaultValue, SyncMode mode) {
        return PlayerDataManager.register(new LongData(id, defaultValue, mode));
    }

    public long add(Player player, long amount) { return update(player, value -> Math.addExact(value, amount)); }
    public long increment(Player player) { return add(player, 1); }
}
