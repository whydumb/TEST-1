package com.kAIS.KAIMyEntity.neoforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class KAIMyEntityConfig {
    public static final ModConfigSpec CONFIG;
    public static ModConfigSpec config;
    public static final ModConfigSpec.BooleanValue openGLEnableLighting;
    public static final ModConfigSpec.IntValue modelPoolMaxCount;
    public static final ModConfigSpec.BooleanValue isMMDShaderEnabled;

    public static final ModConfigSpec.BooleanValue vseeFaceEnabled;
    public static final ModConfigSpec.IntValue vseeFacePort;
    public static final ModConfigSpec.DoubleValue headSensitivity;
    public static final ModConfigSpec.DoubleValue eyeSensitivity;
    public static final ModConfigSpec.BooleanValue smoothingEnabled;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("KAIMyEntity");
        openGLEnableLighting = builder.define("openGLEnableLighting", true);
        modelPoolMaxCount = builder.defineInRange("modelPoolMaxCount", 20, 0, 100);
        isMMDShaderEnabled = builder.define("isMMDShaderEnabled", false);
        builder.pop();
        config = builder.build();

        builder.push("VSeeFace");
        vseeFaceEnabled = builder.define("enabled", false);
        vseeFacePort = builder.defineInRange("port", 39539, 1024, 65535);
        headSensitivity = builder.defineInRange("headSensitivity", 1.0, 0.1, 3.0);
        eyeSensitivity = builder.defineInRange("eyeSensitivity", 1.0, 0.1, 3.0);
        smoothingEnabled = builder.define("smoothingEnabled", true);
        builder.pop();

        CONFIG = builder.build();
    }
}


