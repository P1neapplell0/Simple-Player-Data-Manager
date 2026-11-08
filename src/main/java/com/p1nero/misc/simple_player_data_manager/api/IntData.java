package com.p1nero.misc.simple_player_data_manager.api;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** A registered int value with a namespaced identity and a lazy default. */
public final class IntData extends DataKey<Integer> {
    private IntData(ResourceLocation id, int defaultValue, SyncMode mode) {
        super(id, defaultValue, mode, Tag.TAG_INT, IntTag::valueOf, tag -> ((IntTag) tag).getAsInt());
        encode(defaultValue);
    }

    public static IntData create(ResourceLocation id) { return create(id, 0); }

    public static IntData create(ResourceLocation id, int defaultValue) {
        return create(id, defaultValue, SyncMode.OWNER);
    }

    public static IntData create(ResourceLocation id, int defaultValue, SyncMode mode) {
        return PlayerDataManager.register(new IntData(id, defaultValue, mode));
    }

    public int add(Player player, int amount) { return update(player, value -> Math.addExact(value, amount)); }
    public int increment(Player player) { return add(player, 1); }
}
