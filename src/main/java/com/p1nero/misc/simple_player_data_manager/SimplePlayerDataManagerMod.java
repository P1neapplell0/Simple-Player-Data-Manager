package com.p1nero.misc.simple_player_data_manager;

import com.p1nero.misc.simple_player_data_manager.internal.PlayerCapabilities;
import com.p1nero.misc.simple_player_data_manager.internal.PlayerDataEvents;
import com.p1nero.misc.simple_player_data_manager.network.PlayerDataNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SimplePlayerDataManagerMod.MOD_ID)
public final class SimplePlayerDataManagerMod {
    public static final String MOD_ID = "simple_player_data_manager";

    public SimplePlayerDataManagerMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(PlayerCapabilities::register);
        MinecraftForge.EVENT_BUS.register(PlayerCapabilities.class);
        MinecraftForge.EVENT_BUS.register(PlayerDataEvents.class);
        PlayerDataNetwork.register();
    }
}
