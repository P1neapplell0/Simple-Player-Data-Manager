package com.p1nero.misc.simple_player_data_manager.api;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** A registered String value with a namespaced identity and a lazy default. */
public final class StringData extends DataKey<String> {
    public static final int MAX_LENGTH = 2048;

    @Override
    protected boolean isValid(String value) { return value.length() <= MAX_LENGTH; }

    private StringData(ResourceLocation id, String defaultValue, SyncMode mode) {
        super(id, defaultValue, mode, Tag.TAG_STRING, value -> {
                    if (value.length() > MAX_LENGTH) throw new IllegalArgumentException("String data exceeds " + MAX_LENGTH + " characters");
                    return StringTag.valueOf(value);
                }, Tag::getAsString);
        encode(defaultValue);
    }

    public static StringData create(ResourceLocation id) { return create(id, ""); }

    public static StringData create(ResourceLocation id, String defaultValue) {
        return create(id, defaultValue, SyncMode.OWNER);
    }

    public static StringData create(ResourceLocation id, String defaultValue, SyncMode mode) {
        return PlayerDataManager.register(new StringData(id, defaultValue, mode));
    }
}
