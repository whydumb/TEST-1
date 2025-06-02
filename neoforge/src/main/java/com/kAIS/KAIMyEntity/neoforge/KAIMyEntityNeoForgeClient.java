package com.kAIS.KAIMyEntity.neoforge;

import com.kAIS.KAIMyEntity.KAIMyEntity;
import com.kAIS.KAIMyEntity.KAIMyEntityClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = KAIMyEntity.MOD_ID)
public class KAIMyEntityNeoForgeClient {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        KAIMyEntityClient.logger.info("KAIMyEntity InitClient begin (minimal)...");

        // 최소한의 클라이언트 초기화만
        KAIMyEntityClient.initClient();

        KAIMyEntityClient.logger.info("KAIMyEntity InitClient successful (minimal).");
    }
}