package com.kAIS.KAIMyEntity.neoforge;

import com.kAIS.KAIMyEntity.KAIMyEntity;
import com.kAIS.KAIMyEntity.neoforge.register.KAIMyEntityRegisterCommon;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(KAIMyEntity.MOD_ID)
public class KAIMyEntityNeoForge {
    public static final Logger logger = LogManager.getLogger();

    public KAIMyEntityNeoForge(IEventBus eventBus, ModContainer container) {
        logger.info("KAIMyEntity Init begin...");
        eventBus.addListener(KAIMyEntityRegisterCommon::Register);

        // 설정 등록 제거 (크래시 원인)
        // container.registerConfig(ModConfig.Type.CLIENT, KAIMyEntityConfig.config);

        logger.info("KAIMyEntity Init successful.");
    }
}