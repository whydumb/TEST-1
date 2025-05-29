package com.kAIS.KAIMyEntity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class KAIMyEntityRegisterClient {
    static final Logger logger = LogManager.getLogger();

    public static void Register() {
        logger.info("KAIMyEntityRegisterClient.Register() - Common 모듈에서는 플랫폼별 구현을 사용합니다.");
        // Common 모듈에서는 실제 키바인딩 등록을 하지 않음
        // 각 플랫폼(Fabric, NeoForge)에서 별도로 구현
    }
}