package com.p1nero.misc.simple_player_data_manager.api;

import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Immutable typed schema entry. Register once during mod construction on both physical sides. */
public abstract class DataKey<T> {
    private final ResourceLocation id;
    private final T defaultValue;
    private final SyncMode syncMode;
    private final int tagType;
    private final Function<T, Tag> encoder;
    private final Function<Tag, T> decoder;

    protected DataKey(ResourceLocation id, T defaultValue, SyncMode syncMode, int tagType,
                      Function<T, Tag> encoder, Function<Tag, T> decoder) {
        this.id = Objects.requireNonNull(id);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.syncMode = Objects.requireNonNull(syncMode);
        this.tagType = tagType;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public final ResourceLocation id() { return id; }
    public final T defaultValue() { return defaultValue; }
    public final SyncMode syncMode() { return syncMode; }

    /** Reads on either logical side; missing or incompatible saved values return the declared default. */
    public final T get(Player player) { return PlayerDataManager.get(player, this); }

    /** Writes on the server thread only; equal writes are no-ops, and deltas flush once per tick. */
    public final boolean set(Player player, T value) { return PlayerDataManager.set(player, this, value); }

    /** Performs a read-modify-write on the server thread, returning the resulting value. */
    public final T update(Player player, UnaryOperator<T> update) {
        PlayerDataManager.requireServerThread(player);
        T value = Objects.requireNonNull(update).apply(get(player));
        set(player, value);
        return value;
    }

    /** Removes an explicit value and mirrors the declared default to the owner. */
    public final boolean reset(Player player) { return PlayerDataManager.reset(player, this); }

    public final boolean isPresent(Player player) { return PlayerDataManager.isPresent(player, this); }

    /** Internal serialization hook; applications should use get/set instead. */
    public final Tag encode(T value) { return encoder.apply(Objects.requireNonNull(value)); }

    /** Internal deserialization hook; mismatched NBT types never coerce to a different schema type. */
    public final T decode(Tag tag) { return accepts(tag) ? decoder.apply(tag) : defaultValue; }

    public final boolean accepts(Tag tag) {
        return tag != null && tag.getId() == tagType && isValid(decoder.apply(tag));
    }

    protected boolean isValid(T value) { return true; }
}
