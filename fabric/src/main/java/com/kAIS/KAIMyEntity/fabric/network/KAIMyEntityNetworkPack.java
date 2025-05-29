package com.kAIS.KAIMyEntity.fabric.network;

import com.kAIS.KAIMyEntity.renderer.KAIMyEntityRendererPlayerHelper;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record KAIMyEntityNetworkPack(int opCode, GameProfile profile, int customAnimId) implements CustomPacketPayload {
    public static final Logger logger = LogManager.getLogger();
    public static final CustomPacketPayload.Type<KAIMyEntityNetworkPack> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("kaimyentity", "networkpack"));
    public static final StreamCodec<ByteBuf, KAIMyEntityNetworkPack> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        KAIMyEntityNetworkPack::opCode,
        ByteBufCodecs.GAME_PROFILE,
        KAIMyEntityNetworkPack::profile,
        ByteBufCodecs.VAR_INT,
        KAIMyEntityNetworkPack::customAnimId,
        KAIMyEntityNetworkPack::new
    );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void sendToServer(int opCode, GameProfile profile, int customAnimId){
        ClientPlayNetworking.send(new KAIMyEntityNetworkPack(opCode, profile, customAnimId));
    }

    public static void DoInClient(int opCode, GameProfile profile, int customAnimId) {
        Minecraft MCinstance = Minecraft.getInstance();
        //Ignore message when player is self.
        assert MCinstance.player != null;
        Player targetPlayer = MCinstance.level.getPlayerByUUID(profile.getId());
        if (targetPlayer == null){
            logger.warn("received an invalid profile.");
            return;
        }
        if (profile.equals(MCinstance.player.getGameProfile()))
            return;
        switch (opCode) {
            case 1: {
                MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + MCinstance.player.getName().getString());
                assert MCinstance.level != null;
                if (m != null && targetPlayer != null)
                    KAIMyEntityRendererPlayerHelper.CustomAnim(targetPlayer, Integer.toString(customAnimId));
                break;
            }
            case 2: {
                MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + MCinstance.player.getName().getString());
                assert MCinstance.level != null;
                if (m != null && targetPlayer != null)
                    KAIMyEntityRendererPlayerHelper.ResetPhysics(targetPlayer);
                break;
            }
        }
    }
}
