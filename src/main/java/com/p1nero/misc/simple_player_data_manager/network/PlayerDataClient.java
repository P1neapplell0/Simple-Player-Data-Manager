package com.p1nero.misc.simple_player_data_manager.network;

import com.p1nero.misc.simple_player_data_manager.internal.PlayerCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Loaded only by the physical-client packet handler, never by a dedicated server. */
final class PlayerDataClient {
    private PlayerDataClient() {}

    static void handle(PlayerDataPayload packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getUUID().equals(packet.playerId())) {
            PlayerCapabilities.get(player).apply(PlayerDataNetwork.filter(packet.values(), true), packet.replace());
        }
    }
}
