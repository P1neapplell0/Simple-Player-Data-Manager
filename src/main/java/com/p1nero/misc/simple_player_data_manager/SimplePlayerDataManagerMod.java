package com.p1nero.misc.simple_player_data_manager;

import com.p1nero.misc.simple_player_data_manager.internal.PlayerAttachments;
import com.p1nero.misc.simple_player_data_manager.internal.PlayerDataEvents;
import com.p1nero.misc.simple_player_data_manager.network.PlayerDataNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SimplePlayerDataManagerMod.MOD_ID)
public final class SimplePlayerDataManagerMod {
    public static final String MOD_ID = "simple_player_data_manager";

    public SimplePlayerDataManagerMod(IEventBus modBus) {
        PlayerAttachments.TYPES.register(modBus);
        modBus.addListener(PlayerDataNetwork::register);
        NeoForge.EVENT_BUS.register(PlayerDataEvents.class);
    }
}
