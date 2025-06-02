package com.kAIS.KAIMyEntity.vseeFace;

import com.kAIS.KAIMyEntity.NativeFunc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VSeeFaceManager {
    private static final Logger logger = LogManager.getLogger();
    private static VSeeFaceManager instance;

    private VSeeFaceReceiver receiver;
    private boolean isEnabled = false;
    private final int oscPort;

    // 설정값들
    private float headSensitivity = 1.0f;
    private float eyeSensitivity = 1.0f;
    private float mouthSensitivity = 1.0f;
    private boolean smoothing = true;
    private float smoothingFactor = 0.8f;

    // 스무딩을 위한 이전 값들
    private float prevHeadX = 0.0f, prevHeadY = 0.0f, prevHeadZ = 0.0f;

    private VSeeFaceManager(int port) {
        this.oscPort = port;
        this.receiver = new VSeeFaceReceiver(port);
    }

    public static VSeeFaceManager getInstance() {
        if (instance == null) {
            instance = new VSeeFaceManager(39539); // VSeeFace 기본 포트
        }
        return instance;
    }

    public void toggleVSeeFace() {
        if (isEnabled) {
            stopVSeeFace();
        } else {
            startVSeeFace();
        }
    }

    public void startVSeeFace() {
        if (!isEnabled) {
            receiver.startReceiving();
            isEnabled = true;
            logger.info("VSeeFace tracking enabled");
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("VSeeFace 페이셜 트래킹 시작 (포트: " + oscPort + ")")
            );
        }
    }

    public void stopVSeeFace() {
        if (isEnabled) {
            receiver.stopReceiving();
            isEnabled = false;
            logger.info("VSeeFace tracking disabled");
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("VSeeFace 페이셜 트래킹 중지")
            );
        }
    }

    // VSeeFaceManager.java의 applyFaceDataToModel 메서드 수정

    public void applyFaceDataToModel(long modelPtr) {
        if (!isEnabled || !receiver.isRunning()) return;

        try {
            VSeeFaceReceiver.VSeeFaceData data = receiver.getCurrentFaceData();
            NativeFunc nf = NativeFunc.GetInst();

            // 네이티브 함수 호출 전 유효성 검사
            if (modelPtr == 0) {
                logger.warn("Invalid model pointer");
                return;
            }

            // 머리 각도 계산 (기존 코드의 headX, headY, headZ 변수들을 실제 data에서 가져오도록 수정)
            float headX = data.headRotX * headSensitivity;
            float headY = data.headRotY * headSensitivity;
            float headZ = data.headRotZ * headSensitivity;

            if (smoothing) {
                headX = lerp(prevHeadX, headX, smoothingFactor);
                headY = lerp(prevHeadY, headY, smoothingFactor);
                headZ = lerp(prevHeadZ, headZ, smoothingFactor);

                prevHeadX = headX;
                prevHeadY = headY;
                prevHeadZ = headZ;
            }

            // 머리 각도 제한
            headX = Math.max(-50.0f, Math.min(50.0f, headX));
            headY = Math.max(-80.0f, Math.min(80.0f, headY));
            headZ = Math.max(-30.0f, Math.min(30.0f, headZ));

            // ❌ 문제: SetHeadAngle 함수가 NativeFunc.java에 정의되어 있지 않음
            // 현재로는 주석 처리하거나 NativeFunc.java에 함수를 추가해야 함

            // 임시 해결책 1: 기존 SetBoneRotation 사용 (이미 정의되어 있음)
            nf.SetBoneRotation(modelPtr, "頭",
                    headX * ((float)Math.PI / 180F),
                    headY * ((float)Math.PI / 180F),
                    headZ * ((float)Math.PI / 180F));

            // 표정 적용도 기존 함수들 사용
            nf.SetMorphWeight(modelPtr, "まばたき", data.eyeLeftBlink);
            nf.SetMorphWeight(modelPtr, "ウィンク２右", data.eyeRightBlink);
            nf.SetMorphWeight(modelPtr, "あ", data.mouthA);
            nf.SetMorphWeight(modelPtr, "い", data.mouthI);

        } catch (Exception e) {
            logger.error("Failed to apply VSeeFace data", e);
            // 에러 발생 시 VSeeFace 비활성화
            stopVSeeFace();
        }
    }

    // 추가로 필요한 메서드들
    private void reconnectVSeeFace() {
        logger.info("Attempting to reconnect VSeeFace...");
        stopVSeeFace();
        try {
            Thread.sleep(1000); // 1초 대기
            startVSeeFace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void applyFacialExpressions(long modelPtr, VSeeFaceReceiver.VSeeFaceData data) {
        // 현재는 로깅만, 추후 네이티브 함수 확장 시 실제 모프 적용
        if (data.eyeLeftBlink > 0.5f || data.eyeRightBlink > 0.5f) {
            // 눈 깜빡임 적용
            logger.debug("Eye blink detected: L={}, R={}", data.eyeLeftBlink, data.eyeRightBlink);
        }

        if (data.mouthA > 0.3f) {
            // 입 벌리기 적용
            logger.debug("Mouth open: {}", data.mouthA);
        }

        if (data.mouthI > 0.3f) {
            // 웃음 적용
            logger.debug("Smile: {}", data.mouthI);
        }

        // TODO: 네이티브 함수 확장 후 실제 모프 적용
        // nf.SetMorphWeight(modelPtr, "eyeBlinkLeft", data.eyeLeftBlink);
        // nf.SetMorphWeight(modelPtr, "eyeBlinkRight", data.eyeRightBlink);
        // nf.SetMorphWeight(modelPtr, "mouthOpen", data.mouthA);
        // nf.SetMorphWeight(modelPtr, "mouthSmile", data.mouthI);
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    // Getter/Setter 메서드들
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setHeadSensitivity(float sensitivity) {
        this.headSensitivity = Math.max(0.1f, Math.min(3.0f, sensitivity));
    }

    public void setEyeSensitivity(float sensitivity) {
        this.eyeSensitivity = Math.max(0.1f, Math.min(3.0f, sensitivity));
    }

    public void setMouthSensitivity(float sensitivity) {
        this.mouthSensitivity = Math.max(0.1f, Math.min(3.0f, sensitivity));
    }

    public void setSmoothing(boolean enabled) {
        this.smoothing = enabled;
    }

    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = Math.max(0.1f, Math.min(1.0f, factor));
    }

    public float getHeadSensitivity() { return headSensitivity; }
    public float getEyeSensitivity() { return eyeSensitivity; }
    public float getMouthSensitivity() { return mouthSensitivity; }
    public boolean isSmoothing() { return smoothing; }
    public float getSmoothingFactor() { return smoothingFactor; }

    public void shutdown() {
        if (receiver != null) {
            receiver.stopReceiving();
        }
    }
}