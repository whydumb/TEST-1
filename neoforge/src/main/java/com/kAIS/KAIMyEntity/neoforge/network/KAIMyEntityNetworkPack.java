package com.kAIS.KAIMyEntity.neoforge.network;

import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record KAIMyEntityNetworkPack(int opCode, GameProfile profile, int customAnimId) implements CustomPacketPayload{
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
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void DoInClient(KAIMyEntityNetworkPack pack, IPayloadContext context) {
        Minecraft MCinstance = Minecraft.getInstance();
        //Ignore message when player is self.
        assert MCinstance.player != null;
        assert MCinstance.level != null;
        Player targetPlayer = MCinstance.level.getPlayerByUUID(pack.profile.getId());
        if (targetPlayer == null){
            logger.warn("received an invalid profile.");
            return;
        }
        if (pack.profile.equals(MCinstance.player.getGameProfile()))
            return;

        switch (pack.opCode) {
            case 1: {
                RenderSystem.recordRenderCall(()->{
                    MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + targetPlayer.getName().getString());
                    if (m != null) {
                        // TODO: 커스텀 애니메이션 기능 구현 예정
                        logger.info("Custom animation " + pack.customAnimId + " for player " + targetPlayer.getName().getString());
                    }
                });
                break;
            }
            case 2: {
                RenderSystem.recordRenderCall(()->{
                    MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + targetPlayer.getName().getString());
                    if (m != null) {
                        // TODO: 물리 리셋 기능 구현 예정
                        logger.info("Reset physics for player " + targetPlayer.getName().getString());
                    }
                });
                break;
            }
        }
    }

    public static void DoInServer(KAIMyEntityNetworkPack pack, IPayloadContext context){
        PacketDistributor.sendToAllPlayers(pack);
    }
}