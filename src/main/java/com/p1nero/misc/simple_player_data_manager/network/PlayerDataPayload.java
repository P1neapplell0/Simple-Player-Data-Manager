package com.p1nero.misc.simple_player_data_manager.network;

import com.p1nero.misc.simple_player_data_manager.SimplePlayerDataManagerMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PlayerDataPayload(UUID playerId, boolean replace, CompoundTag values) implements CustomPacketPayload {
    public static final Type<PlayerDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            SimplePlayerDataManagerMod.MOD_ID, "player_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerDataPayload> CODEC = new StreamCodec<>() {
        @Override
        public PlayerDataPayload decode(RegistryFriendlyByteBuf buffer) {
            return new PlayerDataPayload(buffer.readUUID(), buffer.readBoolean(), buffer.readNbt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PlayerDataPayload packet) {
            buffer.writeUUID(packet.playerId());
            buffer.writeBoolean(packet.replace());
            buffer.writeNbt(packet.values());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
