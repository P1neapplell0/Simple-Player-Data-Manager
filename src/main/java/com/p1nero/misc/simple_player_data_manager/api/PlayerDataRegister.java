package com.p1nero.misc.simple_player_data_manager.api;

import net.minecraft.resources.ResourceLocation;

/** Namespace-scoped factory. Construction registers keys immediately; no event-bus binding is needed. */
public final class PlayerDataRegister {
    private final String namespace;

    private PlayerDataRegister(String namespace) {
        ResourceLocation.fromNamespaceAndPath(namespace, "validation");
        this.namespace = namespace;
    }

    public static PlayerDataRegister create(String namespace) { return new PlayerDataRegister(namespace); }
    public ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(namespace, path); }
    public BoolData boolData(String path) { return boolData(path, false); }
    public BoolData boolData(String path, boolean value) { return BoolData.create(id(path), value); }
    public IntData intData(String path) { return intData(path, 0); }
    public IntData intData(String path, int value) { return IntData.create(id(path), value); }
    public LongData longData(String path) { return longData(path, 0L); }
    public LongData longData(String path, long value) { return LongData.create(id(path), value); }
    public DoubleData doubleData(String path) { return doubleData(path, 0.0); }
    public DoubleData doubleData(String path, double value) { return DoubleData.create(id(path), value); }
    public StringData stringData(String path) { return stringData(path, ""); }
    public StringData stringData(String path, String value) { return StringData.create(id(path), value); }
    public BoolData boolData(String path, boolean value, SyncMode mode) { return BoolData.create(id(path), value, mode); }
    public IntData intData(String path, int value, SyncMode mode) { return IntData.create(id(path), value, mode); }
    public LongData longData(String path, long value, SyncMode mode) { return LongData.create(id(path), value, mode); }
    public DoubleData doubleData(String path, double value, SyncMode mode) { return DoubleData.create(id(path), value, mode); }
    public StringData stringData(String path, String value, SyncMode mode) { return StringData.create(id(path), value, mode); }
}
