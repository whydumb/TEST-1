package com.kAIS.KAIMyEntity.neoforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class KAIMyEntityConfig {
    public static final ModConfigSpec CONFIG;
    public static ModConfigSpec config;

    public static final ModConfigSpec.BooleanValue isMMDShaderEnabled;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("KAIMyEntity");
        isMMDShaderEnabled = builder.define("isMMDShaderEnabled", false);
        builder.pop();

        CONFIG = builder.build();
        config = CONFIG;
    }

    // 안전한 접근 메서드
    public static boolean isMMDShaderEnabledSafe() {
        return false; // 항상 false 반환
    }
}