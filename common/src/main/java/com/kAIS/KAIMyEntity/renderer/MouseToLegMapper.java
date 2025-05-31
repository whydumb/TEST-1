package com.kAIS.KAIMyEntity.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class MouseToLegMapper {
    private static boolean legControlMode = false;
    private static float legSensitivity = 0.5f;

    public static void toggleLegControlMode() {
        legControlMode = !legControlMode;
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("다리 제어 모드: " + (legControlMode ? "ON" : "OFF"))
        );
    }

    public static boolean isLegControlMode() {
        return legControlMode;
    }

    public static void setLegControlMode(boolean enabled) {
        legControlMode = enabled;
    }

    public static LegAngles calculateLegAngles(AbstractClientPlayer player, float tickDelta) {
        if (!legControlMode) return new LegAngles(); // 기본값 반환

        // 마우스 입력을 다리 각도로 변환
        float mouseX = player.getXRot();  // 상하 마우스 (무릎 굽힘)
        float mouseY = player.getYRot();  // 좌우 마우스 (고관절 회전)

        // 각도 범위 제한 및 변환
        float hipRotationY = Mth.clamp(mouseY * legSensitivity, -45.0f, 45.0f);
        float hipRotationX = Mth.clamp(mouseX * legSensitivity * 0.3f, -20.0f, 20.0f);
        float kneeRotation = Mth.clamp(-mouseX * legSensitivity, 0.0f, 90.0f); // 무릎은 한 방향만

        // 발목은 무릎 각도에 따라 자동 조정 (자연스러운 보행을 위해)
        float ankleRotation = kneeRotation * 0.5f;

        return new LegAngles(hipRotationX, hipRotationY, 0, kneeRotation, ankleRotation, 0);
    }

    public static class LegAngles {
        public final float hipX, hipY, hipZ;
        public final float kneeX;
        public final float ankleX, ankleY;

        public LegAngles() {
            this(0, 0, 0, 0, 0, 0);
        }

        public LegAngles(float hipX, float hipY, float hipZ, float kneeX, float ankleX, float ankleY) {
            this.hipX = hipX;
            this.hipY = hipY;
            this.hipZ = hipZ;
            this.kneeX = kneeX;
            this.ankleX = ankleX;
            this.ankleY = ankleY;
        }
    }
}