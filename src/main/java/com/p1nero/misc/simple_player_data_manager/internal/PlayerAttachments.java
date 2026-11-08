package com.p1nero.misc.simple_player_data_manager.internal;

import com.p1nero.misc.simple_player_data_manager.SimplePlayerDataManagerMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class PlayerAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES, SimplePlayerDataManagerMod.MOD_ID);
    public static final Supplier<AttachmentType<PlayerDataStore>> DATA = TYPES.register("player_data",
            () -> AttachmentType.builder(PlayerDataStore::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, PlayerDataStore>() {
                        @Override
                        public PlayerDataStore read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            PlayerDataStore data = new PlayerDataStore();
                            data.load(tag);
                            return data;
                        }

                        @Override
                        public CompoundTag write(PlayerDataStore data, HolderLookup.Provider provider) { return data.save(); }
                    }).copyOnDeath().build());

    private PlayerAttachments() {}
    public static PlayerDataStore get(Player player) { return player.getData(DATA.get()); }
}
