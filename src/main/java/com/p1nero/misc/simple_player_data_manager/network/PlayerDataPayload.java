package com.p1nero.misc.simple_player_data_manager.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record PlayerDataPayload(UUID playerId, boolean replace, CompoundTag values) {
    public static PlayerDataPayload decode(FriendlyByteBuf buffer) {
        return new PlayerDataPayload(buffer.readUUID(), buffer.readBoolean(), buffer.readNbt());
    }

    public static void encode(PlayerDataPayload packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId());
        buffer.writeBoolean(packet.replace());
        buffer.writeNbt(packet.values());
    }
}
