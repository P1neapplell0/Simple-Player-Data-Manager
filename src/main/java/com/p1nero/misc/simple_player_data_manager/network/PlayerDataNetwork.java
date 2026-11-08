package com.p1nero.misc.simple_player_data_manager.network;

import com.p1nero.misc.simple_player_data_manager.api.DataKey;
import com.p1nero.misc.simple_player_data_manager.api.PlayerDataManager;
import com.p1nero.misc.simple_player_data_manager.api.SyncMode;
import com.p1nero.misc.simple_player_data_manager.SimplePlayerDataManagerMod;
import com.p1nero.misc.simple_player_data_manager.internal.PlayerDataStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PlayerDataNetwork {
    private static final int KEYS_PER_PACKET = 32;

    private PlayerDataNetwork() {}

    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SimplePlayerDataManagerMod.MOD_ID, "player_data"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(PlayerDataPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerDataPayload::encode)
                .decoder(PlayerDataPayload::decode)
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PlayerDataClient.handle(packet));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void snapshot(ServerPlayer player, PlayerDataStore data) {
        send(player, filter(data.save(), false), true);
        data.clearDirty();
    }

    public static void flush(ServerPlayer player, PlayerDataStore data) {
        if (!data.isDirty()) return;
        send(player, data.pending(), false);
        data.clearDirty();
    }

    static CompoundTag filter(CompoundTag values, boolean allowRemovals) {
        CompoundTag result = new CompoundTag();
        if (values == null) return result;
        for (String name : values.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            DataKey<?> key = id == null ? null : PlayerDataManager.find(id);
            Tag value = values.get(name);
            boolean removal = allowRemovals && value instanceof CompoundTag compound && compound.isEmpty();
            if (key != null && key.syncMode() == SyncMode.OWNER && (removal || key.accepts(value))) {
                result.put(name, value.copy());
            }
        }
        return result;
    }

    private static void send(ServerPlayer player, CompoundTag values, boolean replace) {
        CompoundTag batch = new CompoundTag();
        int count = 0;
        for (String name : values.getAllKeys()) {
            batch.put(name, values.get(name).copy());
            if (++count == KEYS_PER_PACKET) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerDataPayload(player.getUUID(), replace, batch));
                replace = false;
                batch = new CompoundTag();
                count = 0;
            }
        }
        if (replace || count > 0) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerDataPayload(player.getUUID(), replace, batch));
    }
}
