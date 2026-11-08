package com.p1nero.misc.simple_player_data_manager.api;

import com.p1nero.misc.simple_player_data_manager.internal.PlayerAttachments;
import com.p1nero.misc.simple_player_data_manager.internal.PlayerDataStore;
import com.p1nero.misc.simple_player_data_manager.network.PlayerDataNetwork;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Entry point for typed, persistent, server-authoritative player data. */
public final class PlayerDataManager {
    private static final Map<ResourceLocation, DataKey<?>> KEYS = new ConcurrentHashMap<>();

    private PlayerDataManager() {}

    static synchronized <T extends DataKey<?>> T register(T key) {
        if (key.id().toString().length() > 256) throw new IllegalArgumentException("Data key exceeds 256 characters");
        if (KEYS.containsKey(key.id())) throw new IllegalArgumentException("Duplicate player data key: " + key.id());
        if (KEYS.size() >= 4096) throw new IllegalStateException("Too many player data keys (maximum 4096)");
        KEYS.put(key.id(), key);
        return key;
    }

    /** Looks up schema metadata, or null if the key was never registered. */
    public static DataKey<?> find(ResourceLocation id) { return KEYS.get(id); }
    public static Collection<DataKey<?>> registeredKeys() { return List.copyOf(KEYS.values()); }

    public static <T> T get(Player player, DataKey<T> key) {
        checkRegistered(key);
        if (player.level().isClientSide() && key.syncMode() == SyncMode.SERVER_ONLY) return key.defaultValue();
        return key.decode(store(player).get(key.id().toString()));
    }

    /** Returns whether a valid explicit value exists, rather than just an effective default. */
    public static boolean isPresent(Player player, DataKey<?> key) {
        checkRegistered(key);
        if (player.level().isClientSide() && key.syncMode() == SyncMode.SERVER_ONLY) return false;
        return key.accepts(store(player).get(key.id().toString()));
    }

    public static <T> boolean set(Player player, DataKey<T> key, T value) {
        requireServerThread(player);
        checkRegistered(key);
        Tag encoded = key.encode(value);
        PlayerDataStore data = store(player);
        if (encoded.equals(data.get(key.id().toString()))) return false;
        if (!isPresent(player, key) && value.equals(key.defaultValue())) return false;
        data.put(key.id().toString(), encoded, key.syncMode() == SyncMode.OWNER);
        return true;
    }

    public static boolean reset(Player player, DataKey<?> key) {
        requireServerThread(player);
        checkRegistered(key);
        return store(player).remove(key.id().toString(), key.syncMode() == SyncMode.OWNER);
    }

    /** Forces a full owner snapshot, normally automatic on login, respawn and dimension change. */
    public static void sync(ServerPlayer player) {
        requireServerThread(player);
        PlayerDataNetwork.snapshot(player, store(player));
    }

    /** Flushes pending per-key deltas immediately instead of waiting for the next player tick. */
    public static void flush(ServerPlayer player) {
        requireServerThread(player);
        PlayerDataNetwork.flush(player, store(player));
    }

    public static void requireServerThread(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.server.isSameThread()) {
            throw new IllegalStateException("Player data writes require the logical server thread");
        }
    }

    private static void checkRegistered(DataKey<?> key) {
        if (KEYS.get(key.id()) != key) throw new IllegalArgumentException("Unregistered player data key: " + key.id());
    }

    private static PlayerDataStore store(Player player) { return PlayerAttachments.get(player); }
}
