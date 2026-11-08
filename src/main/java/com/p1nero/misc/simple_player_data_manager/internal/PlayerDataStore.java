package com.p1nero.misc.simple_player_data_manager.internal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Internal mutable state owned by exactly one player on one logical side. */
public final class PlayerDataStore {
    private CompoundTag values = new CompoundTag();
    private CompoundTag dirty = new CompoundTag();

    public Tag get(String key) { return values.get(key); }

    public void put(String key, Tag value, boolean synchronize) {
        values.put(key, value);
        if (synchronize) dirty.put(key, value.copy());
    }

    public boolean remove(String key, boolean synchronize) {
        if (!values.contains(key)) return false;
        values.remove(key);
        if (synchronize) dirty.put(key, new CompoundTag());
        return true;
    }

    public CompoundTag save() { return values.copy(); }

    public void load(CompoundTag tag) {
        values = tag.copy();
        dirty = new CompoundTag();
    }

    public boolean isDirty() { return !dirty.isEmpty(); }
    public CompoundTag pending() { return dirty.copy(); }
    public void clearDirty() { dirty = new CompoundTag(); }

    public void apply(CompoundTag changes, boolean replace) {
        if (replace) values = new CompoundTag();
        for (String key : changes.getAllKeys()) {
            Tag value = changes.get(key);
            if (value instanceof CompoundTag compound && compound.isEmpty()) values.remove(key);
            else values.put(key, value.copy());
        }
    }
}
