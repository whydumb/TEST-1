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

    // 연결 상태 체크
    private long lastDataReceived = 0;
    private static final long DATA_TIMEOUT_MS = 5000; // 5초

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
            try {
                receiver.startReceiving();
                if (receiver.isRunning()) {
                    isEnabled = true;
                    lastDataReceived = System.currentTimeMillis();
                    logger.info("VSeeFace tracking enabled on port {}", oscPort);
                    Minecraft.getInstance().gui.getChat().addMessage(
                            Component.literal("§aVSeeFace 페이셜 트래킹 시작 (포트: " + oscPort + ")")
                    );
                } else {
                    if (!receiver.isOSCAvailable()) {
                        logger.warn("OSC library not available - VSeeFace functionality disabled");
                        Minecraft.getInstance().gui.getChat().addMessage(
                                Component.literal("§eVSeeFace: OSC 라이브러리가 필요합니다. 모드를 다시 빌드해주세요.")
                        );
                    } else {
                        logger.error("Failed to start VSeeFace receiver");
                        Minecraft.getInstance().gui.getChat().addMessage(
                                Component.literal("§cVSeeFace 시작 실패! 포트 " + oscPort + "가 사용 중이거나 VSeeFace가 실행되지 않았습니다.")
                        );
                    }
                }
            } catch (Exception e) {
                logger.error("Error starting VSeeFace", e);
                Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("§cVSeeFace 시작 중 오류 발생: " + e.getMessage())
                );
            }
        }
    }

    public void stopVSeeFace() {
        if (isEnabled) {
            try {
                receiver.stopReceiving();
                isEnabled = false;
                logger.info("VSeeFace tracking disabled");
                Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("§eVSeeFace 페이셜 트래킹 중지")
                );
            } catch (Exception e) {
                logger.error("Error stopping VSeeFace", e);
            }
        }
    }

    /**
     * VSeeFace 데이터를 모델에 적용
     */
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

            // 데이터가 수신되고 있는지 확인
            if (hasValidData(data)) {
                lastDataReceived = System.currentTimeMillis();
            }

            // 머리 각도 계산 및 적용
            applyHeadRotation(modelPtr, data, nf);

            // 표정 적용
            applyFacialExpressions(modelPtr, data, nf);

        } catch (Exception e) {
            logger.error("Failed to apply VSeeFace data to model", e);
            // 연속적인 에러 발생 시 자동 중지
            stopVSeeFace();
        }
    }

    /**
     * 머리 회전 적용
     */
    private void applyHeadRotation(long modelPtr, VSeeFaceReceiver.VSeeFaceData data, NativeFunc nf) {
        try {
            // 머리 각도 계산
            float headX = data.headRotX * headSensitivity;
            float headY = data.headRotY * headSensitivity;
            float headZ = data.headRotZ * headSensitivity;

            // 스무딩 적용
            if (smoothing) {
                headX = lerp(prevHeadX, headX, 1.0f - smoothingFactor);
                headY = lerp(prevHeadY, headY, 1.0f - smoothingFactor);
                headZ = lerp(prevHeadZ, headZ, 1.0f - smoothingFactor);

                prevHeadX = headX;
                prevHeadY = headY;
                prevHeadZ = headZ;
            }

            // 머리 각도 제한
            headX = Math.max(-50.0f, Math.min(50.0f, headX));
            headY = Math.max(-80.0f, Math.min(80.0f, headY));
            headZ = Math.max(-30.0f, Math.min(30.0f, headZ));

            // 본 회전 적용 (MMD의 일반적인 본 이름들)
            // 먼저 일본어 본 이름으로 시도
            try {
                nf.SetBoneRotation(modelPtr, "頭",
                        headX * ((float)Math.PI / 180F),
                        headY * ((float)Math.PI / 180F),
                        headZ * ((float)Math.PI / 180F));
            } catch (Exception e1) {
                // 일본어 본 이름 실패 시 영어로 시도
                try {
                    nf.SetBoneRotation(modelPtr, "Head",
                            headX * ((float)Math.PI / 180F),
                            headY * ((float)Math.PI / 180F),
                            headZ * ((float)Math.PI / 180F));
                } catch (Exception e2) {
                    // 모두 실패 시 로그 출력
                    logger.debug("Failed to apply head rotation to both Japanese and English bone names");
                }
            }

        } catch (Exception e) {
            logger.debug("Error applying head rotation", e);
        }
    }

    /**
     * 표정 적용
     */
    private void applyFacialExpressions(long modelPtr, VSeeFaceReceiver.VSeeFaceData data, NativeFunc nf) {
        try {
            // 눈 깜빡임 적용
            if (data.eyeLeftBlink > 0.01f) {
                applyMorph(nf, modelPtr, "まばたき", data.eyeLeftBlink * eyeSensitivity);
                applyMorph(nf, modelPtr, "Blink", data.eyeLeftBlink * eyeSensitivity);
                applyMorph(nf, modelPtr, "eyeBlinkLeft", data.eyeLeftBlink * eyeSensitivity);
            }

            if (data.eyeRightBlink > 0.01f) {
                applyMorph(nf, modelPtr, "ウィンク", data.eyeRightBlink * eyeSensitivity);
                applyMorph(nf, modelPtr, "ウィンク２右", data.eyeRightBlink * eyeSensitivity);
                applyMorph(nf, modelPtr, "Wink_R", data.eyeRightBlink * eyeSensitivity);
                applyMorph(nf, modelPtr, "eyeBlinkRight", data.eyeRightBlink * eyeSensitivity);
            }

            // 입 모양 적용
            if (data.mouthA > 0.01f) {
                applyMorph(nf, modelPtr, "あ", data.mouthA * mouthSensitivity);
                applyMorph(nf, modelPtr, "a", data.mouthA * mouthSensitivity);
                applyMorph(nf, modelPtr, "mouthOpen", data.mouthA * mouthSensitivity);
            }

            if (data.mouthI > 0.01f) {
                applyMorph(nf, modelPtr, "い", data.mouthI * mouthSensitivity);
                applyMorph(nf, modelPtr, "i", data.mouthI * mouthSensitivity);
                applyMorph(nf, modelPtr, "mouthSmile", data.mouthI * mouthSensitivity);
            }

            if (data.mouthU > 0.01f) {
                applyMorph(nf, modelPtr, "う", data.mouthU * mouthSensitivity);
                applyMorph(nf, modelPtr, "u", data.mouthU * mouthSensitivity);
                applyMorph(nf, modelPtr, "mouthPucker", data.mouthU * mouthSensitivity);
            }

            if (data.mouthE > 0.01f) {
                applyMorph(nf, modelPtr, "え", data.mouthE * mouthSensitivity);
                applyMorph(nf, modelPtr, "e", data.mouthE * mouthSensitivity);
            }

            if (data.mouthO > 0.01f) {
                applyMorph(nf, modelPtr, "お", data.mouthO * mouthSensitivity);
                applyMorph(nf, modelPtr, "o", data.mouthO * mouthSensitivity);
                applyMorph(nf, modelPtr, "mouthFunnel", data.mouthO * mouthSensitivity);
            }

        } catch (Exception e) {
            logger.debug("Error applying facial expressions", e);
        }
    }

    /**
     * 모프 적용 (에러 무시)
     */
    private void applyMorph(NativeFunc nf, long modelPtr, String morphName, float weight) {
        try {
            // 가중치 제한
            weight = Math.max(0.0f, Math.min(1.0f, weight));
            nf.SetMorphWeight(modelPtr, morphName, weight);
        } catch (Exception e) {
            // 모프가 존재하지 않을 수 있으므로 에러 무시
            logger.trace("Morph '{}' not found or failed to apply", morphName);
        }
    }

    /**
     * 유효한 데이터인지 확인
     */
    private boolean hasValidData(VSeeFaceReceiver.VSeeFaceData data) {
        return Math.abs(data.headRotX) > 0.1f || Math.abs(data.headRotY) > 0.1f || Math.abs(data.headRotZ) > 0.1f ||
                data.eyeLeftBlink > 0.01f || data.eyeRightBlink > 0.01f ||
                data.mouthA > 0.01f || data.mouthI > 0.01f || data.mouthU > 0.01f || data.mouthE > 0.01f || data.mouthO > 0.01f;
    }

    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        if (!isEnabled || !receiver.isRunning()) {
            return false;
        }

        // 최근에 데이터를 받았는지 확인
        long timeSinceLastData = System.currentTimeMillis() - lastDataReceived;
        return timeSinceLastData < DATA_TIMEOUT_MS;
    }

    /**
     * 재연결 시도
     */
    public void reconnect() {
        logger.info("Attempting to reconnect VSeeFace...");
        stopVSeeFace();
        try {
            Thread.sleep(1000); // 1초 대기
            startVSeeFace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 선형 보간
     */
    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    // Getter/Setter 메서드들
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setHeadSensitivity(float sensitivity) {
        this.headSensitivity = Math.max(0.1f, Math.min(3.0f, sensitivity));
        logger.debug("Head sensitivity set to: {}", this.headSensitivity);
    }

    public void setEyeSensitivity(float sensitivity) {
        this.eyeSensitivity = Math.max(0.1f, Math.min(3.0f, sensitivity));
        logger.debug("Eye sensitivity set to: {}", this.eyeSensitivity);
    }

    public void setMouthSensitivity(float sensitivity) {
        this.mouthSensitivity = Math.max(0.1f, Math.min(3.0f, sensitivity));
        logger.debug("Mouth sensitivity set to: {}", this.mouthSensitivity);
    }

    public void setSmoothing(boolean enabled) {
        this.smoothing = enabled;
        logger.debug("Smoothing set to: {}", enabled);
    }

    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = Math.max(0.1f, Math.min(1.0f, factor));
        logger.debug("Smoothing factor set to: {}", this.smoothingFactor);
    }

    public float getHeadSensitivity() {
        return headSensitivity;
    }

    public float getEyeSensitivity() {
        return eyeSensitivity;
    }

    public float getMouthSensitivity() {
        return mouthSensitivity;
    }

    public boolean isSmoothing() {
        return smoothing;
    }

    public float getSmoothingFactor() {
        return smoothingFactor;
    }

    public int getPort() {
        return oscPort;
    }

    public VSeeFaceReceiver.VSeeFaceData getCurrentData() {
        if (receiver != null) {
            return receiver.getCurrentFaceData();
        }
        return null;
    }

    /**
     * 상태 정보 문자열 반환
     */
    public String getStatusString() {
        if (!isEnabled) {
            return "§cVSeeFace 비활성화";
        } else if (!receiver.isRunning()) {
            return "§eVSeeFace 수신기 중지됨";
        } else if (!isConnected()) {
            return "§6VSeeFace 연결 끊김 (데이터 없음)";
        } else {
            return "§aVSeeFace 활성화 및 연결됨";
        }
    }

    public void shutdown() {
        if (receiver != null) {
            receiver.stopReceiving();
        }
        isEnabled = false;
        logger.info("VSeeFaceManager shutdown complete");
    }
}