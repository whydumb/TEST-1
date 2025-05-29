package com.kAIS.KAIMyEntity.fabric.register;

import com.kAIS.KAIMyEntity.fabric.network.KAIMyEntityNetworkPack;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class KAIMyEntityRegisterCommon {

    public static void Register() {
        PayloadTypeRegistry.playS2C().register(KAIMyEntityNetworkPack.TYPE, KAIMyEntityNetworkPack.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(KAIMyEntityNetworkPack.TYPE, KAIMyEntityNetworkPack.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KAIMyEntityNetworkPack.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                for(ServerPlayer serverPlayer : PlayerLookup.all(context.server())){
                    if(!serverPlayer.getGameProfile().equals(payload.profile())){
                        ServerPlayNetworking.send(serverPlayer, payload);
                    }
                }
            });
        });
    }
}
