package com.p1nero.misc.simple_player_data_manager.api;

import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** A registered double value with a namespaced identity and a lazy default. */
public final class DoubleData extends DataKey<Double> {
    @Override
    protected boolean isValid(Double value) { return Double.isFinite(value); }

    private DoubleData(ResourceLocation id, double defaultValue, SyncMode mode) {
        super(id, defaultValue, mode, Tag.TAG_DOUBLE, value -> {
                    if (!Double.isFinite(value)) throw new IllegalArgumentException("Double data must be finite");
                    return DoubleTag.valueOf(value);
                }, tag -> ((DoubleTag) tag).getAsDouble());
        encode(defaultValue);
    }

    public static DoubleData create(ResourceLocation id) { return create(id, 0.0); }

    public static DoubleData create(ResourceLocation id, double defaultValue) {
        return create(id, defaultValue, SyncMode.OWNER);
    }

    public static DoubleData create(ResourceLocation id, double defaultValue, SyncMode mode) {
        return PlayerDataManager.register(new DoubleData(id, defaultValue, mode));
    }

    public double add(Player player, double amount) { return update(player, value -> value + amount); }
}
