package com.p1nero.misc.simple_player_data_manager.internal;

import com.p1nero.misc.simple_player_data_manager.SimplePlayerDataManagerMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PlayerCapabilities {
    public static final Capability<PlayerDataStore> DATA = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SimplePlayerDataManagerMod.MOD_ID, "player_data");

    private PlayerCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) { event.register(PlayerDataStore.class); }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider provider = new Provider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            get(event.getEntity()).load(get(original).save());
        } finally {
            original.invalidateCaps();
        }
    }

    public static PlayerDataStore get(Player player) {
        return player.getCapability(DATA).orElseThrow(() -> new IllegalStateException("Missing player data capability"));
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerDataStore data = new PlayerDataStore();
        private LazyOptional<PlayerDataStore> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == DATA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() { return data.save(); }

        @Override
        public void deserializeNBT(CompoundTag tag) { data.load(tag); }

        private void invalidate() {
            optional.invalidate();
            optional = LazyOptional.of(() -> data);
        }
    }
}
